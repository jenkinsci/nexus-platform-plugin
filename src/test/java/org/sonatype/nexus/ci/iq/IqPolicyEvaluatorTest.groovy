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
import com.sonatype.nexus.api.iq.Action
import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation
import com.sonatype.nexus.api.iq.PolicyAlert
import com.sonatype.nexus.api.iq.ProprietaryConfig
import com.sonatype.nexus.api.iq.internal.InternalIqClient
import com.sonatype.nexus.api.iq.scan.ScanResult

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.NxiqConfiguration

import hudson.EnvVars
import hudson.FilePath
import hudson.Launcher
import hudson.model.Job
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import hudson.remoting.Channel
import org.slf4j.Logger
import spock.lang.Specification
import spock.util.mop.ConfineMetaClassChanges

import static TestDataGenerators.createAlert
import static java.util.Collections.emptyList

@ConfineMetaClassChanges([IqClientFactory])
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

  def envVars = new EnvVars(['SCAN_PATTERN': 'some-scan-pattern', 'MODULE_EXCLUDE': 'some-module-exclude'])

  def run = Mock(Run)

  def job = Mock(Job)

  def reportUrl = 'http://server/report'

  def setup() {
    GroovyMock(NxiqConfiguration, global: true)
    GroovyMock(GlobalNexusConfiguration, global: true)
    GroovyMock(IqClientFactory, global: true)
    GroovyMock(RemoteScannerFactory, global: true)
    NxiqConfiguration.serverUrl >> URI.create("http://server/path")
    NxiqConfiguration.credentialsId >> '123-cred-456'
    GlobalNexusConfiguration.instanceId >> 'instance-id'
    iqClient.evaluateApplication("appId", "stage", _) >> new ApplicationPolicyEvaluation(0, 0, 0, 0, [],
        reportUrl)
    IqClientFactory.getIqClient(*_) >> iqClient
    remoteScanResult.copyToLocalScanResult() >> scanResult
    run.getEnvironment(_) >> envVars
    run.parent >> job
  }

  def 'it retrieves proprietary config followed by remote scan followed by evaluation in correct order (happy path)'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep("stage", new SelectedApplication('appId'), [new ScanPattern("*.jar")], [],
          false, null)
      def evaluationResult = new ApplicationPolicyEvaluation(0, 0, 0, 0, emptyList(), reportUrl)
      def remoteScanner = Mock(RemoteScanner)

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then: 'retrieves proprietary config'
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.getProprietaryConfigForApplicationEvaluation('appId') >> proprietaryConfig

    then: 'performs a remote scan'
      1 * RemoteScannerFactory.getRemoteScanner("appId", "stage", ["*.jar"], [], workspace,
          proprietaryConfig, _ as Logger, 'instance-id') >> remoteScanner
      1 * channel.call(remoteScanner) >> remoteScanResult

    then: 'evaluates the result'
      1 * iqClient.evaluateApplication("appId", "stage", scanResult) >> evaluationResult
  }

  def 'it expands environment variables for scan pattern'() {
    setup:
      def remoteScanner = Mock(RemoteScanner)
      def buildStep = new IqPolicyEvaluatorBuildStep("stage", new SelectedApplication('appId'),
          [new ScanPattern('/path1/$SCAN_PATTERN/path2/')],
          [], false, "131-cred")

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * RemoteScannerFactory.
          getRemoteScanner("appId", "stage", ['/path1/some-scan-pattern/path2/'], _, workspace, _, _ as Logger,
              'instance-id') >> remoteScanner
  }

  def 'it ignores when no environment variables set for scan pattern'() {
    setup:
      def remoteScanner = Mock(RemoteScanner)
      def buildStep = new IqPolicyEvaluatorBuildStep("stage", new SelectedApplication('appId'),
          [new ScanPattern('/path1/$NONEXISTENT_SCAN_PATTERN/path2/')], [], false, "131-cred")

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * RemoteScannerFactory.
          getRemoteScanner("appId", "stage", ['/path1/$NONEXISTENT_SCAN_PATTERN/path2/'], _, workspace, _, _ as Logger,
              'instance-id') >> remoteScanner
  }

  def 'it passes module excludes to the remote scanner'() {
    setup:
      def remoteScanner = Mock(RemoteScanner)
      def buildStep = new IqPolicyEvaluatorBuildStep("stage", new SelectedApplication('appId'), [],
          [new ModuleExclude('/path1/$NONEXISTENT_MODULE_EXCLUDE/path2/')], false, "131-cred")

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * RemoteScannerFactory.getRemoteScanner(*_) >> { arguments ->
        assert arguments[3] == ['/path1/$NONEXISTENT_MODULE_EXCLUDE/path2/']
        remoteScanner
      }
  }

  def 'it expands environment variables for module exclude'() {
    setup:
      def remoteScanner = Mock(RemoteScanner)
      def buildStep = new IqPolicyEvaluatorBuildStep("stage", new SelectedApplication('appId'), [],
          [new ModuleExclude('/path1/$MODULE_EXCLUDE/path2/')], false, "131-cred")

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * RemoteScannerFactory.getRemoteScanner(*_) >> { arguments ->
        assert arguments[3] == ['/path1/some-module-exclude/path2/']
        remoteScanner
      }
  }

  def 'exception handling (part 1)'() {
    setup:
      iqClient.getProprietaryConfigForApplicationEvaluation('appId') >> { throw exception }
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('appId'), [new ScanPattern('*.jar')], [],
          failBuildOnNetworkError, '131-cred')

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
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
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('appId'), [new ScanPattern('*.jar')], [],
          failBuildOnNetworkError, '131-cred')
      PrintStream logger = Mock()
      TaskListener listener = Mock() {
        getLogger() >> logger
      }

    when:
      buildStep.perform(run, workspace, launcher, listener)

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
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
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('appId'), [new ScanPattern('*.jar')], [],
          false,
          '131-cred')
      RemoteScanner remoteScanner = Mock()
      RemoteScannerFactory.getRemoteScanner(*_) >> remoteScanner

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * channel.call(remoteScanner) >> { throw new IOException('CRASH') }
      IOException e = thrown()
      e.message == 'CRASH'
  }

  def 'evaluation networking exceptions are suppressed by failBuildOnNetworkError'() {
    setup:
      def failBuildOnNetworkError = false
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('appId'), [new ScanPattern('*.jar')], [],
          failBuildOnNetworkError, '131-cred')

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult) >>
          { throw new IqClientException('SNAP', new IOException('CRASH')) }
      noExceptionThrown()
  }

  def 'global no credentials are passed to the client builder when no job credentials provided'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('appId'), [new ScanPattern('*.jar')], [],
          true,
          jobCredentials)

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * IqClientFactory.getIqClient { it.credentialsId == jobCredentials } >> iqClient

    where:
      jobCredentials << [ null, '', '131-cred']
  }

  def 'evaluation result outcome determines build status'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('appId'), [new ScanPattern('*.jar')], [],
          false,
          '131-cred')

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult) >>
          new ApplicationPolicyEvaluation(0, 0, 0, 0, alerts, reportUrl)
      1 * run.setResult(buildResult)

    where:
      alerts                                                  || buildResult
      []                                                      || Result.SUCCESS
      [new PolicyAlert(null, [new Action(Action.ID_WARN)])]   || Result.UNSTABLE
      [new PolicyAlert(null, [new Action(Action.ID_NOTIFY)])] || Result.SUCCESS
  }

  def 'evaluation throws exception when build results in failure'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('appId'), [new ScanPattern('*.jar')], [],
          false,
          '131-cred')
      def policyEvaluation = new ApplicationPolicyEvaluation(0, 0, 0, 0,
          [new PolicyAlert(null, [new Action(Action.ID_FAIL)])], reportUrl)

    when:
      buildStep.perform(run, workspace, launcher, Mock(TaskListener))

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult) >> policyEvaluation
      1 * run.setResult(Result.FAILURE)

    and:
      PolicyEvaluationException ex = thrown()
      ex.message == 'IQ Server evaluation of application appId failed'
      ex.policyEvaluation == policyEvaluation
  }

  def 'prints an error message on failure'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('appId'), [new ScanPattern('*.jar')], [],
          false,
          '131-cred')
      TaskListener listener = Mock()
      PrintStream log = Mock()
      listener.getLogger() >> log
      def trigger = createAlert(Action.ID_FAIL).trigger

    when:
      buildStep.perform(run, workspace, launcher, listener)

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult) >>
          new ApplicationPolicyEvaluation(0, 1, 2, 3, [new PolicyAlert(trigger, [new Action(Action.ID_FAIL)])],
              reportUrl)

    and:
      thrown PolicyEvaluationException
      1 * log.println(
          'Nexus IQ reports policy failing due to \nPolicy(policyName) [\n Component(displayName=value, ' +
              'hash=12hash34) [\n  Constraint(constraintName) [summary because: reason] ]]\nThe detailed report can be' +
              ' viewed online at http://server/report\nSummary of policy violations: 1 critical, 2 severe, 3 moderate')
      1 * listener.fatalError('IQ Server evaluation of application appId failed')
  }

  def 'prints a log message on warnings'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('appId'), [new ScanPattern('*.jar')], [],
          false,
          '131-cred')
      TaskListener listener = Mock()
      PrintStream log = Mock()
      listener.getLogger() >> log
      def trigger = createAlert(Action.ID_FAIL).trigger

    when:
      buildStep.perform(run, workspace, launcher, listener)

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult) >>
          new ApplicationPolicyEvaluation(0, 1, 2, 3, [new PolicyAlert(trigger, [new Action(Action.ID_WARN)])],
              reportUrl)
      1 * log.println("IQ Server evaluation of application appId detected warnings")
      1 * log.println(
          'Nexus IQ reports policy warning due to \nPolicy(policyName) [\n Component(displayName=value, ' +
              'hash=12hash34) [\n  Constraint(constraintName) [summary because: reason] ]]\nThe detailed report can be' +
              ' viewed online at http://server/report\nSummary of policy violations: 1 critical, 2 severe, 3 moderate')
  }

  def 'prints a summary on success'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('appId'), [new ScanPattern('*.jar')], [],
          false,
          '131-cred')
      TaskListener listener = Mock()
      PrintStream log = Mock()
      listener.getLogger() >> log

    when:
      buildStep.perform(run, workspace, launcher, listener)

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult) >>
          new ApplicationPolicyEvaluation(0, 0, 0, 0, [],
              reportUrl)
      0 * log.println("WARNING: IQ Server evaluation of application appId detected warnings.")
      1 * log.println('\nThe detailed report can be viewed online at http://server/report\n' +
          'Summary of policy violations: 0 critical, 0 severe, 0 moderate')
  }
}
