/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import javax.annotation.ParametersAreNonnullByDefault

import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation
import com.sonatype.nexus.ci.config.GlobalNexusConfiguration
import com.sonatype.nexus.ci.config.NxiqConfiguration
import com.sonatype.nexus.ci.util.LoggerBridge

import hudson.FilePath
import hudson.Launcher
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import org.apache.commons.lang.exception.ExceptionUtils

import static com.google.common.base.Preconditions.checkArgument

@ParametersAreNonnullByDefault
trait IqPolicyEvaluator
{
  String iqStage

  String iqApplication

  List<ScanPattern> iqScanPatterns

  Boolean failBuildOnNetworkError

  String jobCredentialsId

  private static final List<String> DEFAULT_SCAN_PATTERN =
      ['**/*.jar', '**/*.war', '**/*.ear', '**/*.zip', '**/*.tar.gz']

  ApplicationPolicyEvaluation evaluatePolicy(final Run run,
                                             final FilePath workspace,
                                             final Launcher launcher,
                                             final TaskListener listener)
  {
    try {
      checkArgument(iqStage && iqApplication, 'Arguments iqApplication and iqStage are mandatory')
      LoggerBridge loggerBridge = new LoggerBridge(listener)
      loggerBridge.debug(Messages.IqPolicyEvaluation_Evaluating())

      def credentialsId = getCredentials()
      def iqClient = IqClientFactory.getIqClient(loggerBridge, credentialsId)
      def scanPatterns = getPatterns(iqScanPatterns, listener, run)

      def proprietaryConfig =
          rethrowNetworkErrors { iqClient.getProprietaryConfigForApplicationEvaluation(iqApplication) }
      def remoteScanner = RemoteScannerFactory.getRemoteScanner(iqApplication, iqStage, scanPatterns, workspace,
          NxiqConfiguration.serverUrl, proprietaryConfig, loggerBridge, GlobalNexusConfiguration.instanceId)
      def scanResult = launcher.getChannel().call(remoteScanner).copyToLocalScanResult()

      def evaluationResult = rethrowNetworkErrors { iqClient.evaluateApplication(iqApplication, iqStage, scanResult) }

      Result result = handleEvaluationResult(evaluationResult, listener, iqApplication)
      run.setResult(result)

      def healthAction = new PolicyEvaluationHealthAction(run, evaluationResult)
      run.addAction(healthAction)

      return evaluationResult
    } catch (IqNetworkException e) {
      return handleNetworkException(e, listener, run)
    }
  }

  def handleNetworkException(final IqNetworkException e, final TaskListener listener, final Run run) {
    if (failBuildOnNetworkError) {
      throw e.cause
    }
    else {
      listener.logger.println Messages.IqPolicyEvaluation_UnableToCommunicate(e.message)
      run.result = Result.UNSTABLE
      return null
    }
  }

  private String getCredentials() {
    NxiqConfiguration.isPkiAuthentication ?
        null :
        (jobCredentialsId ?: NxiqConfiguration.credentialsId)
  }

  @SuppressWarnings('CatchException') // all exceptions are rethrown and possibly modified
  private <T> T rethrowNetworkErrors(final Closure<T> closure) {
    try {
      closure()
    }
    catch (Exception e) {
      if (isNetworkError(e)) {
        throw new IqNetworkException(e.getMessage(), e)
      }
      else {
        throw e
      }
    }
  }

  private boolean isNetworkError(final Exception throwable) {
    ExceptionUtils.indexOfType(throwable, IOException) >= 0
  }

  private List<String> getPatterns(final List<ScanPattern> iqScanPatterns, final TaskListener listener, final Run run) {
    def envVars = run.getEnvironment(listener)
    iqScanPatterns.collect { envVars.expand(it.scanPattern) } - null - '' ?: DEFAULT_SCAN_PATTERN
  }

  private Result handleEvaluationResult(final ApplicationPolicyEvaluation evaluationResult,
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
