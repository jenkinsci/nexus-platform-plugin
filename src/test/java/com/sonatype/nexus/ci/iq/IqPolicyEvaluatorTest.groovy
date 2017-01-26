/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import com.sonatype.clm.dto.model.component.ComponentIdentifier
import com.sonatype.clm.dto.model.policy.ComponentFact
import com.sonatype.nexus.api.exception.IqClientException
import com.sonatype.nexus.api.iq.Action
import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation
import com.sonatype.nexus.api.iq.PolicyAlert
import com.sonatype.nexus.api.iq.PolicyFact
import com.sonatype.nexus.api.iq.ProprietaryConfig
import com.sonatype.nexus.api.iq.internal.InternalIqClient
import com.sonatype.nexus.api.iq.scan.ScanResult
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
    GroovyMock(com.sonatype.nexus.ci.config.NxiqConfiguration, global: true)
    GroovyMock(IqClientFactory, global: true)
    GroovyMock(RemoteScannerFactory, global: true)
    RemoteScannerFactory.getRemoteScanner("appId", "stage", _, workspace, "http://server/path", _, _) >> Mock(RemoteScanner)
    NxiqConfiguration.serverUrl >> URI.create("http://server/path")
    NxiqConfiguration.credentialsId >> '123-cred-456'
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

    then: 'performs a remove scan'
      1 * RemoteScannerFactory.getRemoteScanner("appId", "stage", ["*.jar"], workspace,
          URI.create("http://server/path"), proprietaryConfig, _ as Logger) >> remoteScanner
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
      1 * RemoteScannerFactory.getRemoteScanner("appId", "stage", defaultPatterns, workspace,
          URI.create("http://server/path"), _, _ as Logger) >> remoteScanner
  }

  def 'it expands environment variables for scan pattern'() {
    setup:
      def remoteScanner = Mock(RemoteScanner)
      def buildStep = new IqPolicyEvaluatorBuildStep("stage", "appId", [new ScanPattern('/path1/$SCAN_PATTERN/path2/')],
          false, "131-cred")

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      1 * RemoteScannerFactory.getRemoteScanner("appId", "stage", ['/path1/some-scan-pattern/path2/'], workspace,
          URI.create("http://server/path"), _, _ as Logger) >> remoteScanner
  }

  def 'it ignores when no environment variables set for scan pattern'() {
    setup:
      def remoteScanner = Mock(RemoteScanner)
      def buildStep = new IqPolicyEvaluatorBuildStep("stage", "appId",
          [new ScanPattern('/path1/$NONEXISTENT_SCAN_PATTERN/path2/')], false, "131-cred")

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      1 * RemoteScannerFactory.getRemoteScanner("appId", "stage", ['/path1/$NONEXISTENT_SCAN_PATTERN/path2/'],
          workspace, URI.create("http://server/path"), _, _ as Logger) >> remoteScanner
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
      1 * logger.println('WARNING: Unable to communicate with IQ Server: BOOM!!')

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
      PrintWriter log = Mock()
      def trigger = new PolicyFact('s', 's1', 5, [new ComponentFact(new ComponentIdentifier('s', [k:'v']), 's')])

    when:
      buildStep.perform(run, workspace, launcher, listener)

    then:
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult) >>
          new ApplicationPolicyEvaluation(0, 0, 0, 0, [new PolicyAlert(trigger, [new Action(Action.ID_FAIL)])], false,
              reportUrl)
      1 * listener.fatalError('IQ Server evaluation of application %s failed.', 'appId') >> log
      1 * log.println({ it.startsWith('Triggered by policy alert:') })
  }

  def 'prints an log message on warnings'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', 'appId', [new ScanPattern('*.jar')], false, '131-cred')
      PrintStream logger = Mock()
      TaskListener listener = Mock() {
        getLogger() >> logger
      }
      def result = new ApplicationPolicyEvaluation(0, 0, 0, 0, [new PolicyAlert(null, [new Action(Action.ID_WARN)])],
          false, reportUrl)

    when:
      buildStep.perform(run, workspace, launcher, listener)

    then:
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult) >> result
      1 * logger.println("WARNING: IQ Server evaluation of application appId detected warnings.")
  }
}
