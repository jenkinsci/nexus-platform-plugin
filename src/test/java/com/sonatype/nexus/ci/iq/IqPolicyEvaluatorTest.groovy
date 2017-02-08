/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import com.sonatype.nexus.api.exception.IqClientException
import com.sonatype.nexus.api.iq.Action
import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation
import com.sonatype.nexus.api.iq.PolicyAlert
import com.sonatype.nexus.api.iq.ProprietaryConfig
import com.sonatype.nexus.api.iq.internal.InternalIqClient
import com.sonatype.nexus.api.iq.scan.ScanResult
import com.sonatype.nexus.ci.config.GlobalNexusConfiguration
import com.sonatype.nexus.ci.config.NxiqConfiguration

import hudson.EnvVars
import hudson.FilePath
import hudson.Launcher
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import hudson.remoting.Channel
import org.slf4j.Logger
import spock.lang.Specification

import static com.sonatype.nexus.ci.iq.TestDataGenerators.createAlert
import static java.util.Collections.emptyList

class IqPolicyEvaluatorTest
    extends Specification
{
  def scanResult = Mock(ScanResult)

  def remoteScanResult = Mock(RemoteScanResult)

  def channel = Mock(Channel) {
    call(*_) >> remoteScanResult
  }

  def launcher = Mock(Launcher) {
    getChannel() >> channel
  }

  def workspace = new FilePath(new File("/tmp/path"))

  def iqClient = Mock(InternalIqClient)

  def proprietaryConfig = new ProprietaryConfig(['com.example'], ['^org.*'])

  def envVars = new EnvVars(['SCAN_PATTERN': 'some-scan-pattern'])

  def run = Mock(Run)

  def reportUrl = 'http://server/report'

  def setup() {
    GroovyMock(NxiqConfiguration, global: true)
    GroovyMock(GlobalNexusConfiguration, global: true)
    GroovyMock(IqClientFactory, global: true)
    GroovyMock(RemoteScannerFactory, global: true)
    NxiqConfiguration.serverUrl >> URI.create("http://server/path")
    NxiqConfiguration.credentialsId >> '123-cred-456'
    GlobalNexusConfiguration.instanceId >> 'instance-id'
    iqClient.evaluateApplication("appId", "stage", _) >> new ApplicationPolicyEvaluation(0, 0, 0, 0, [], false,
        reportUrl)
    IqClientFactory.getIqClient(*_) >> iqClient
    remoteScanResult.copyToLocalScanResult() >> scanResult
    run.getEnvironment(_) >> envVars
  }

  def 'it retrieves proprietary config followed by remote scan followed by evaluation in correct order (happy path)'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep("stage", "appId", [new ScanPattern("*.jar")], false, null)
      def evaluationResult = new ApplicationPolicyEvaluation(0, 0, 0, 0, emptyList(), false, reportUrl)
      def remoteScanner = Mock(RemoteScanner)

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then: 'retrieves proprietary config'
      1 * iqClient.getProprietaryConfigForApplicationEvaluation('appId') >> proprietaryConfig

    then: 'performs a remote scan'
      1 * RemoteScannerFactory.getRemoteScanner("appId", "stage", ["*.jar"], workspace,
          proprietaryConfig, _ as Logger, 'instance-id') >> remoteScanner
      1 * channel.call(remoteScanner) >> remoteScanResult

    then: 'evaluates the result'
      1 * iqClient.evaluateApplication("appId", "stage", scanResult) >> evaluationResult
  }

  def 'it falls back to default scan patterns when none are provided'() {
    setup:
      def remoteScanner = Mock(RemoteScanner)
      def buildStep = new IqPolicyEvaluatorBuildStep("stage", "appId", [], false, "131-cred")
      def defaultPatterns = ["**/*.jar", "**/*.war", "**/*.ear", "**/*.zip", "**/*.tar.gz"]

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      1 * RemoteScannerFactory.
          getRemoteScanner("appId", "stage", defaultPatterns, workspace, _, _ as Logger, 'instance-id') >> remoteScanner
  }

  def 'it expands environment variables for scan pattern'() {
    setup:
      def remoteScanner = Mock(RemoteScanner)
      def buildStep = new IqPolicyEvaluatorBuildStep("stage", "appId", [new ScanPattern('/path1/$SCAN_PATTERN/path2/')],
          false, "131-cred")

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      1 * RemoteScannerFactory.
          getRemoteScanner("appId", "stage", ['/path1/some-scan-pattern/path2/'], workspace, _, _ as Logger,
              'instance-id') >> remoteScanner
  }

  def 'it ignores when no environment variables set for scan pattern'() {
    setup:
      def remoteScanner = Mock(RemoteScanner)
      def buildStep = new IqPolicyEvaluatorBuildStep("stage", "appId",
          [new ScanPattern('/path1/$NONEXISTENT_SCAN_PATTERN/path2/')], false, "131-cred")

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      1 * RemoteScannerFactory.
          getRemoteScanner("appId", "stage", ['/path1/$NONEXISTENT_SCAN_PATTERN/path2/'], workspace, _, _ as Logger,
              'instance-id') >> remoteScanner
  }

  def 'exception handling (part 1)'() {
    setup:
      iqClient.getProprietaryConfigForApplicationEvaluation('appId') >> { throw exception }
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', 'appId', [new ScanPattern('*.jar')],
          failBuildOnNetworkError, '131-cred')

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      Exception e = thrown()
      e.class == expectedException
      e.message == expectedMessage

    where:
      exception                       | failBuildOnNetworkError || expectedException | expectedMessage
      new IqClientException('BOOM!!') | true                    || IqClientException | 'BOOM!!'
      new IqClientException('BOOM!!') | false                   || IqClientException | 'BOOM!!'
      new IqClientException('BOOM!!',
          new IOException("CRASH"))   | true                    || IqClientException | 'BOOM!!'
  }

  def 'exception handling (part 2)'() {
    setup:
      iqClient.getProprietaryConfigForApplicationEvaluation('appId') >> { throw exception }
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', 'appId', [new ScanPattern('*.jar')],
          failBuildOnNetworkError, '131-cred')
      PrintStream logger = Mock()
      TaskListener listener = Mock() {
        getLogger() >> logger
      }

    when:
      buildStep.perform(run, workspace, launcher, listener)

    then:
      noExceptionThrown()
      1 * run.setResult(Result.UNSTABLE)
      1 * logger.println('Unable to communicate with IQ Server: BOOM!!')

    where:
      exception                     | failBuildOnNetworkError || expectedException | expectedMessage
      new IqClientException('BOOM!!',
          new IOException("CRASH")) | false                   || null              | 'BOOM!!'
  }

  def 'it throws an exception when remote scanner fails'() {
    setup:
      iqClient.getProprietaryConfigForApplicationEvaluation('appId') >> proprietaryConfig
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', 'appId', [new ScanPattern('*.jar')], false, '131-cred')
      RemoteScanner remoteScanner = Mock()
      RemoteScannerFactory.getRemoteScanner(*_) >> remoteScanner

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      1 * channel.call(remoteScanner) >> { throw new IOException('CRASH') }
      IOException e = thrown()
      e.message == 'CRASH'
  }

  def 'evaluation networking exceptions are suppressed by failBuildOnNetworkError'() {
    setup:
      def failBuildOnNetworkError = false
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', 'appId', [new ScanPattern('*.jar')],
          failBuildOnNetworkError, '131-cred')

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult) >>
          { throw new IqClientException('SNAP', new IOException('CRASH')) }
      noExceptionThrown()
  }

  def 'job specific credentials are passed to the client builder'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', 'appId', [new ScanPattern('*.jar')], true, '131-cred')

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      1 * IqClientFactory.getIqClient(_ as Logger, '131-cred') >> iqClient
  }

  def 'global credentials are passed to the client builder when no job credentials provided'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', 'appId', [new ScanPattern('*.jar')], true, jobCredentials)

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      1 * IqClientFactory.getIqClient(_ as Logger, '123-cred-456') >> iqClient

    where:
      jobCredentials << [ null, '' ]
  }

  def 'null credentials are passed to the client builder when pki auth is true'() {
    setup:
      NxiqConfiguration.isPkiAuthentication >> true
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', 'appId', [new ScanPattern('*.jar')], true, jobCredentials)

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      1 * IqClientFactory.getIqClient(_ as Logger, null) >> iqClient

    where:
      jobCredentials << [ null, '', '131-cred' ]
  }

  def 'evaluation result outcome determines build status'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', 'appId', [new ScanPattern('*.jar')], false, '131-cred')

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult) >>
          new ApplicationPolicyEvaluation(0, 0, 0, 0, alerts, false, reportUrl)
      1 * run.setResult(buildResult)

    where:
      alerts                                                  || buildResult
      []                                                      || Result.SUCCESS
      [new PolicyAlert(null, [new Action(Action.ID_FAIL)])]   || Result.FAILURE
      [new PolicyAlert(null, [new Action(Action.ID_WARN)])]   || Result.UNSTABLE
      [new PolicyAlert(null, [new Action(Action.ID_NOTIFY)])] || Result.SUCCESS
  }

  def 'prints an error message on failure'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', 'appId', [new ScanPattern('*.jar')], false, '131-cred')
      TaskListener listener = Mock()
      PrintStream log = Mock()
      listener.getLogger() >> log
      def trigger = createAlert(Action.ID_FAIL).trigger

    when:
      buildStep.perform(run, workspace, launcher, listener)

    then:
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult) >>
          new ApplicationPolicyEvaluation(0, 1, 2, 3, [new PolicyAlert(trigger, [new Action(Action.ID_FAIL)])], false,
              reportUrl)
      1 * log.println(
          'Nexus IQ reports policy failing due to \nPolicy(policyName) [\n Component(displayName=value, ' +
              'hash=12hash34) [\n  Constraint(constraintName) [summary because: reason] ]]\nThe detailed report can be' +
              ' viewed online at http://server/report\nSummary of policy violations: 1 critical, 2 severe, 3 moderate')
      1 * listener.fatalError('IQ Server evaluation of application appId failed')
  }

  def 'prints a log message on warnings'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', 'appId', [new ScanPattern('*.jar')], false, '131-cred')
      TaskListener listener = Mock()
      PrintStream log = Mock()
      listener.getLogger() >> log
      def trigger = createAlert(Action.ID_FAIL).trigger

    when:
      buildStep.perform(run, workspace, launcher, listener)

    then:
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult) >>
          new ApplicationPolicyEvaluation(0, 1, 2, 3, [new PolicyAlert(trigger, [new Action(Action.ID_WARN)])], false,
              reportUrl)
      1 * log.println("IQ Server evaluation of application appId detected warnings")
      1 * log.println(
          'Nexus IQ reports policy warning due to \nPolicy(policyName) [\n Component(displayName=value, ' +
              'hash=12hash34) [\n  Constraint(constraintName) [summary because: reason] ]]\nThe detailed report can be' +
              ' viewed online at http://server/report\nSummary of policy violations: 1 critical, 2 severe, 3 moderate')
  }

  def 'prints a summary on success'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', 'appId', [new ScanPattern('*.jar')], false, '131-cred')
      TaskListener listener = Mock()
      PrintStream log = Mock()
      listener.getLogger() >> log

    when:
      buildStep.perform(run, workspace, launcher, listener)

    then:
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult) >>
          new ApplicationPolicyEvaluation(0, 0, 0, 0, [], false,
              reportUrl)
      0 * log.println("WARNING: IQ Server evaluation of application appId detected warnings.")
      1 * log.println('\nThe detailed report can be viewed online at http://server/report\n' +
          'Summary of policy violations: 0 critical, 0 severe, 0 moderate')
  }
}
