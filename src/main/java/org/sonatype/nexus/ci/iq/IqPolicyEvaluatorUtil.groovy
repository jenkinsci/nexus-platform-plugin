/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.sonatype.nexus.ci.iq

import java.nio.file.Paths
import java.util.stream.Collectors

import com.sonatype.nexus.api.common.CallflowOptions
import com.sonatype.nexus.api.exception.IqClientException
import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation
import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.NxiqConfiguration
import org.sonatype.nexus.ci.util.IqUtil
import org.sonatype.nexus.ci.util.LoggerBridge

import hudson.AbortException
import hudson.EnvVars
import hudson.FilePath
import hudson.Launcher
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.exception.ExceptionUtils

import static com.google.common.base.Preconditions.checkArgument


class IqPolicyEvaluatorUtil
{
  static final String MINIMAL_SERVER_VERSION_REQUIRED = '1.69.0'

  @SuppressWarnings(['AbcMetric', 'ParameterCount', 'CyclomaticComplexity'])
  static ApplicationPolicyEvaluation evaluatePolicy(final IqPolicyEvaluator iqPolicyEvaluator,
                                                    final Run run,
                                                    final FilePath workspace,
                                                    final Launcher launcher,
                                                    final TaskListener listener,
                                                    final EnvVars envVars)
  {
    ensureInNodeContext(run, workspace, launcher, listener)
    NxiqConfiguration iqConfig = null
    if (iqPolicyEvaluator.iqInstanceId) {
      iqConfig = IqUtil.getIqConfiguration(iqPolicyEvaluator.iqInstanceId)
    }

    iqConfig = iqConfig ?: IqUtil.getFirstIqConfiguration()

    try {
      LoggerBridge loggerBridge = new LoggerBridge(listener)
      if (iqPolicyEvaluator.getEnableDebugLogging()) {
        loggerBridge.setDebugEnabled(true)
      }
      // !!! GET IT HERE
      String applicationId = envVars.expand(iqPolicyEvaluator.getIqApplication()?.applicationId)
      String organizationId = iqPolicyEvaluator.iqOrganization
      String iqStage = iqPolicyEvaluator.iqStage

      checkArgument(iqStage && applicationId, 'Arguments iqApplication and iqStage are mandatory')

      loggerBridge.debug(Messages.IqPolicyEvaluation_Evaluating())

      def iqClient = IqClientFactory.getIqClient(new IqClientFactoryConfiguration(
          serverUrl: (iqConfig == null || iqConfig.serverUrl == null) ? null : new URI(iqConfig.serverUrl),
          credentialsId: iqPolicyEvaluator.jobCredentialsId ?: iqConfig?.credentialsId,
          context: run.parent,
          log: loggerBridge))

      iqClient.validateServerVersion(MINIMAL_SERVER_VERSION_REQUIRED)
      def verified = iqClient.verifyOrCreateApplication(applicationId, organizationId)
      String message =
          StringUtils.isBlank(iqPolicyEvaluator.iqOrganization) ? "The application ID ${applicationId} is invalid." :
              "The application ID ${applicationId} is invalid for organization ID ${organizationId}."
      checkArgument(verified, message)

      def expandedScanPatterns = getScanPatterns(iqPolicyEvaluator.iqScanPatterns, envVars)
      def expandedModuleExcludes = getExpandedModuleExcludes(iqPolicyEvaluator.iqModuleExcludes, envVars)


      def proprietaryConfig = iqClient.getProprietaryConfigForApplicationEvaluation(applicationId)
      def advancedProperties = getAdvancedProperties(iqPolicyEvaluator.advancedProperties, loggerBridge)

      def licensedFeatures = iqClient.getLicensedFeatures()

      def remoteScanner = RemoteScannerFactory.
          getRemoteScanner(applicationId, iqStage, expandedScanPatterns, expandedModuleExcludes,
              workspace, proprietaryConfig, loggerBridge, GlobalNexusConfiguration.instanceId,
              advancedProperties, envVars, licensedFeatures)

      def scanResult
      def evaluationResult
      def remoteScanResult
      try {
        remoteScanResult = launcher.getChannel().call(remoteScanner)
        scanResult = remoteScanResult.copyToLocalScanResult()

        def repositoryUrlFinder = RemoteRepositoryUrlFinderFactory
            .getRepositoryUrlFinder(workspace, loggerBridge, GlobalNexusConfiguration.instanceId, applicationId,
                envVars)
        def repositoryUrl = launcher.getChannel().call(repositoryUrlFinder)
        if (repositoryUrl != null) {
          def repositoryPath = Paths.get(workspace.getRemote(),".git").toString()
          iqClient.addOrUpdateSourceControl(applicationId, repositoryUrl, repositoryPath)
        }

        File workDirectory = new File(workspace.getRemote())

        listener.logger.println("!!! make it so " + expandedScanPatterns)

        Boolean runCallflow = iqPolicyEvaluator.getRunCallflow()

        CallflowOptions callflowOptions
        if (runCallflow) {
          CallflowRunConfiguration callflowRunConfiguration = iqPolicyEvaluator.getCallflowRunConfiguration()
          listener.logger.println("!!! callflowRunConfiguration.getAdditionalConfiguration(): " +
              callflowRunConfiguration.getAdditionalConfiguration())

          callflowOptions = buildCallflowOptions(
              callflowRunConfiguration,
              workDirectory,
              envVars,
              iqPolicyEvaluator.iqScanPatterns
          )

          listener.logger.println("!!! options: " + callflowOptions.scanTargets + ", " + callflowOptions.namespaces)
        } else {
          callflowOptions = null
        }

        listener.logger.println("!!! callflow options: " + callflowOptions)

        evaluationResult = iqClient.evaluateApplication(
            applicationId,
            iqStage,
            scanResult,
            workDirectory,
            null,
            callflowOptions
        )
      } finally {
        // clean up scan files on master and agent
        scanResult?.scanFile?.delete()
        remoteScanResult?.delete()
      }

      def healthAction = new PolicyEvaluationHealthAction(iqConfig?.displayName, iqConfig?.serverUrl, applicationId,
          iqStage, run, evaluationResult)
      run.addAction(healthAction)

      if (!iqConfig?.hideReports) {
        def reportAction = new PolicyEvaluationReportAction(applicationId, iqStage, run, evaluationResult)
        run.addAction(reportAction)
      }

      listener.logger.println("!!! evalResult critical: " + evaluationResult.getCriticalComponentCount())
      listener.logger.println("!!! alerts: " + evaluationResult.getPolicyAlerts().size())
      Result result = handleEvaluationResult(evaluationResult, listener, applicationId, iqConfig?.hideReports)
      listener.logger.println("!!! result: " + result)
      run.setResult(result)
      if (result == Result.FAILURE) {
        listener.logger.println("!!! end with fail action")
        throw new PolicyEvaluationException(Messages.IqPolicyEvaluation_EvaluationFailed(applicationId),
            evaluationResult)
      }

      if (scanResult.scan?.summary?.errorCount > 0) {
        if (iqPolicyEvaluator.failBuildOnScanningErrors) {
          def msg = Messages.IqPolicyEvaluation_EvaluationFailed(applicationId)
          throw new PolicyEvaluationException(msg, evaluationResult)
        }
        run.setResult(Result.UNSTABLE)
      }

      return evaluationResult
    }
    catch (IqClientException e) {
      return handleNetworkException(iqPolicyEvaluator.failBuildOnNetworkError, e, listener, run)
    }
  }

  private static handleNetworkException(final Boolean failBuildOnNetworkError, final IqClientException e,
                                        final TaskListener listener, final Run run)
  {
    def isNetworkError = isNetworkError(e)
    if (!isNetworkError || failBuildOnNetworkError) {
      throw e
    }
    else {
      listener.logger.println ExceptionUtils.getStackTrace(e)
      run.result = Result.UNSTABLE
      return null
    }
  }

  private static boolean isNetworkError(final Exception throwable) {
    ExceptionUtils.indexOfType(throwable, IOException) >= 0
  }

  private static Properties getAdvancedProperties(final String inputPropertiesString, final LoggerBridge loggerBridge) {
    Properties advanced = new Properties()
    if (StringUtils.isNotEmpty(inputPropertiesString)) {
      try {
        new StringReader(inputPropertiesString).withCloseable { advanced.load(it) }
      }
      catch (IOException e) {
        loggerBridge.error("Unable to parse advanced properties: ${e.message}", e)
      }
    }
    return advanced
  }

  private static List<String> getScanPatterns(final List<ScanPattern> iqScanPatterns, final EnvVars envVars)
  {
    iqScanPatterns.collect { envVars.expand(it.scanPattern) } - null - ''
  }

  private static CallflowOptions buildCallflowOptions(
      final CallflowRunConfiguration callflowRunConfiguration,
      final File workdir,
      final EnvVars envVars,
      final List<ScanPattern> iqScanPatterns)
  {
    if (callflowRunConfiguration == null) {
      final List<String> expandedPatterns = getScanPatterns(iqScanPatterns, envVars)
      final List<String> targets = RemoteScanner.getScanTargets(workdir, expandedPatterns)
          .collect { it.getAbsolutePath() }

      // defaults to using same targets as original iq scan, when enabled but no additional config passed
      return new CallflowOptions(targets, null, null)
    } else {
      List<ScanPattern> patterns = callflowRunConfiguration.getCallflowScanPatterns()
      if (patterns == null) {
        // defaults to using same targets as original iq scan, when no patterns passed with addtional config
        patterns = iqScanPatterns
      }

      final List<String> expandedPatterns = getScanPatterns(patterns, envVars)

      final List<String> targets = RemoteScanner.getScanTargets(workdir, expandedPatterns)
          .collect { it.getAbsolutePath() }

      // TODO: Figure out how to pass the additional config
      return new CallflowOptions(targets, callflowRunConfiguration.getCallflowNamespaces(), null)
    }
  }

  private static List<String> getExpandedModuleExcludes(final List<ModuleExclude> moduleExcludes,
                                                        final EnvVars envVars)
  {
    moduleExcludes.collect { envVars.expand(it.moduleExclude) } - null - ''
  }

  private static Result handleEvaluationResult(final ApplicationPolicyEvaluation evaluationResult,
                                               final TaskListener listener,
                                               final String appId,
                                               boolean hideReports)
  {
    def policyFailureMessageFormatter = new PolicyFailureMessageFormatter(evaluationResult)
    if (!hideReports) {
      listener.logger.println("!!! can you see this?")
      listener.logger.println(policyFailureMessageFormatter.message)
    }

    if (policyFailureMessageFormatter.hasFailures()) {
      listener.fatalError(Messages.IqPolicyEvaluation_EvaluationFailed(appId))
      return Result.FAILURE
    }
    else if (policyFailureMessageFormatter.hasWarnings()) {
      listener.logger.println(Messages.IqPolicyEvaluation_EvaluationWarning(appId))
      return Result.UNSTABLE
    }
    else {
      return Result.SUCCESS
    }
  }

  private static ensureInNodeContext(
      final Run run,
      final FilePath workspace,
      final Launcher launcher,
      final TaskListener listener
  ) {
    if (!(launcher && workspace)) {
      run.setResult(Result.FAILURE)
      if (listener) {
        listener.error(Messages.IqPolicyEvaluation_NodeContextRequired())
      }
      throw new AbortException(Messages.IqPolicyEvaluation_NodeContextRequired())
    }
  }
}
