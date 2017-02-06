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

@ParametersAreNonnullByDefault
trait IqPolicyEvaluator
{
  String iqStage

  String iqApplication

  List<ScanPattern> iqScanPatterns

  Boolean failBuildOnNetworkError

  String jobCredentialsId

  private static final List<String> DEFAULT_SCAN_PATTERN =
      ["**/*.jar", "**/*.war", "**/*.ear", "**/*.zip", "**/*.tar.gz"]

  void evaluatePolicy(final Run run,
                      final FilePath workspace,
                      final Launcher launcher,
                      final TaskListener listener)
  {
    try {
      LoggerBridge loggerBridge = new LoggerBridge(listener)

      def credentialsId = NxiqConfiguration.isPkiAuthentication ? null : (jobCredentialsId ?: NxiqConfiguration.credentialsId)
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
    } catch (IqNetworkException e) {
      if (failBuildOnNetworkError) {
        throw e.cause
      }
      else {
        listener.getLogger().println("WARNING: Unable to communicate with IQ Server: " + e.getMessage())
        run.setResult(Result.UNSTABLE)
      }
    }
  }

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
    ExceptionUtils.indexOfType(throwable, IOException.class) >= 0
  }

  private List<String> getPatterns(final List<ScanPattern> iqScanPatterns, final TaskListener listener, final Run run) {
    def envVars = run.getEnvironment(listener)
    iqScanPatterns.collect { envVars.expand(it.scanPattern) } - null - "" ?: DEFAULT_SCAN_PATTERN
  }

  private Result handleEvaluationResult(final ApplicationPolicyEvaluation evaluationResult,
                                        final TaskListener listener,
                                        final String appId)
  {
    def policyFailureMessageFormatter = new PolicyFailureMessageFormatter(evaluationResult)
    listener.logger.println(policyFailureMessageFormatter.message)

    if (policyFailureMessageFormatter.hasFailures()) {
      listener.fatalError("IQ Server evaluation of application %s failed.", appId)
      return Result.FAILURE
    }
    else if (policyFailureMessageFormatter.hasWarnings()) {
      listener.logger.println("WARNING: IQ Server evaluation of application " + appId + " detected warnings.")
      return Result.UNSTABLE
    }
    else {
      return Result.SUCCESS
    }
  }
}
