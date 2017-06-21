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

import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.util.LoggerBridge

import hudson.FilePath
import hudson.Launcher
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import org.apache.commons.lang.exception.ExceptionUtils

import static com.google.common.base.Preconditions.checkArgument

class IqPolicyEvaluatorUtil
{
  private static final List<String> DEFAULT_SCAN_PATTERN =
      ['**/*.jar', '**/*.war', '**/*.ear', '**/*.zip', '**/*.tar.gz']

  @SuppressWarnings('AbcMetric')
  static ApplicationPolicyEvaluation evaluatePolicy(final IqPolicyEvaluator iqPolicyEvaluator,
                                                    final Run run,
                                                    final FilePath workspace,
                                                    final Launcher launcher,
                                                    final TaskListener listener)
  {
    try {
      checkArgument(iqPolicyEvaluator.iqStage && iqPolicyEvaluator.iqApplication,
          'Arguments iqApplication and iqStage are mandatory')
      LoggerBridge loggerBridge = new LoggerBridge(listener)
      loggerBridge.debug(Messages.IqPolicyEvaluation_Evaluating())

      def iqClient = IqClientFactory.getIqClient(new IqClientFactoryConfiguration(
          credentialsId: iqPolicyEvaluator.jobCredentialsId, context: run.parent, log: loggerBridge))

      def scanPatterns = getPatterns(iqPolicyEvaluator.iqScanPatterns, listener, run)

      def proprietaryConfig =
          rethrowNetworkErrors {
            iqClient.getProprietaryConfigForApplicationEvaluation(iqPolicyEvaluator.iqApplication)
          }
      def remoteScanner = RemoteScannerFactory.
          getRemoteScanner(iqPolicyEvaluator.iqApplication, iqPolicyEvaluator.iqStage, scanPatterns, workspace,
              proprietaryConfig, loggerBridge, GlobalNexusConfiguration.instanceId)
      def scanResult = launcher.getChannel().call(remoteScanner).copyToLocalScanResult()

      def evaluationResult = rethrowNetworkErrors {
        iqClient.evaluateApplication(iqPolicyEvaluator.iqApplication, iqPolicyEvaluator.iqStage, scanResult)
      }

      Result result = handleEvaluationResult(evaluationResult, listener, iqPolicyEvaluator.iqApplication)
      run.setResult(result)

      def healthAction = new PolicyEvaluationHealthAction(run, evaluationResult)
      run.addAction(healthAction)

      return evaluationResult
    }
    catch (IqNetworkException e) {
      return handleNetworkException(iqPolicyEvaluator.failBuildOnNetworkError, e, listener, run)
    }
  }

  private static handleNetworkException(final boolean failBuildOnNetworkError, final IqNetworkException e,
                                    final TaskListener listener, final Run run)
  {
    if (failBuildOnNetworkError) {
      throw e.cause
    }
    else {
      listener.logger.println Messages.IqPolicyEvaluation_UnableToCommunicate(e.message)
      run.result = Result.UNSTABLE
      return null
    }
  }

  @SuppressWarnings('CatchException')
  // all exceptions are rethrown and possibly modified
  private static <T> T rethrowNetworkErrors(final Closure<T> closure) {
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

  private static boolean isNetworkError(final Exception throwable) {
    ExceptionUtils.indexOfType(throwable, IOException) >= 0
  }

  private static List<String> getPatterns(final List<ScanPattern> iqScanPatterns, final TaskListener listener,
                                          final Run run)
  {
    def envVars = run.getEnvironment(listener)
    iqScanPatterns.collect { envVars.expand(it.scanPattern) } - null - '' ?: DEFAULT_SCAN_PATTERN
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
