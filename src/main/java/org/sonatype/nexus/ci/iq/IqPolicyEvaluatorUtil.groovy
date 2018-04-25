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

import com.sonatype.nexus.api.exception.IqClientException
import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.util.LoggerBridge

import hudson.EnvVars
import hudson.FilePath
import hudson.Launcher
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import org.apache.commons.lang.exception.ExceptionUtils

import static com.google.common.base.Preconditions.checkArgument

class IqPolicyEvaluatorUtil
{
  @SuppressWarnings('AbcMetric')
  static ApplicationPolicyEvaluation evaluatePolicy(final IqPolicyEvaluator iqPolicyEvaluator,
                                                    final Run run,
                                                    final FilePath workspace,
                                                    final Launcher launcher,
                                                    final TaskListener listener)
  {
    try {
      String applicationId = iqPolicyEvaluator.getIqApplication()?.applicationId

      checkArgument(iqPolicyEvaluator.iqStage && applicationId, 'Arguments iqApplication and iqStage are mandatory')

      LoggerBridge loggerBridge = new LoggerBridge(listener)
      loggerBridge.debug(Messages.IqPolicyEvaluation_Evaluating())

      def iqClient = IqClientFactory.getIqClient(
          new IqClientFactoryConfiguration(credentialsId: iqPolicyEvaluator.jobCredentialsId, context: run.parent,
              log: loggerBridge))

      def verified = iqClient.verifyOrCreateApplication(applicationId)
      checkArgument(verified, 'The application ID ' + applicationId + ' is invalid.')

      def envVars = run.getEnvironment(listener)
      def expandedScanPatterns = getScanPatterns(iqPolicyEvaluator.iqScanPatterns, envVars)
      def expandedModuleExcludes = getExpandedModuleExcludes(iqPolicyEvaluator.moduleExcludes, envVars)

      def proprietaryConfig = iqClient.getProprietaryConfigForApplicationEvaluation(applicationId)
      def remoteScanner = RemoteScannerFactory.
          getRemoteScanner(applicationId, iqPolicyEvaluator.iqStage, expandedScanPatterns, expandedModuleExcludes,
              workspace, proprietaryConfig, loggerBridge, GlobalNexusConfiguration.instanceId)
      def scanResult = launcher.getChannel().call(remoteScanner).copyToLocalScanResult()

      def evaluationResult = iqClient.evaluateApplication(applicationId, iqPolicyEvaluator.iqStage, scanResult)

      def healthAction = new PolicyEvaluationHealthAction(run, evaluationResult)
      run.addAction(healthAction)

      Result result = handleEvaluationResult(evaluationResult, listener, applicationId)
      run.setResult(result)
      if (result == Result.FAILURE) {
        throw new PolicyEvaluationException(Messages.IqPolicyEvaluation_EvaluationFailed(applicationId),
            evaluationResult)
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

  private static List<String> getScanPatterns(final List<ScanPattern> iqScanPatterns, final EnvVars envVars)
  {
    iqScanPatterns.collect { envVars.expand(it.scanPattern) } - null - ''
  }

  private static List<String> getExpandedModuleExcludes(final List<ModuleExclude> moduleExcludes,
                                                        final EnvVars envVars)
  {
    moduleExcludes.collect { envVars.expand(it.moduleExclude) } - null - ''
  }

  private static Result handleEvaluationResult(final ApplicationPolicyEvaluation evaluationResult,
                                               final TaskListener listener,
                                               final String appId)
  {
    def policyFailureMessageFormatter = new PolicyFailureMessageFormatter(evaluationResult)
    listener.logger.println(policyFailureMessageFormatter.message)

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
}
