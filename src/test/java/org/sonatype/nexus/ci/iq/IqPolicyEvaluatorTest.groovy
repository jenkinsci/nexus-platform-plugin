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

import com.sonatype.insight.scan.model.Scan
import com.sonatype.insight.scan.model.ScanSummary
import com.sonatype.nexus.api.common.CallflowOptions
import com.sonatype.nexus.api.exception.IqClientException
import com.sonatype.nexus.api.iq.Action
import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation
import com.sonatype.nexus.api.iq.PolicyAlert
import com.sonatype.nexus.api.iq.ProprietaryConfig
import com.sonatype.nexus.api.iq.internal.InternalIqClient
import com.sonatype.nexus.api.iq.scan.ScanResult

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.NxiqConfiguration

import hudson.AbortException
import hudson.EnvVars
import hudson.FilePath
import hudson.Launcher
import hudson.model.AbstractBuild
import hudson.model.AbstractProject
import hudson.model.BuildListener
import hudson.model.Result
import hudson.remoting.Channel
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import org.slf4j.Logger
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.mop.ConfineMetaClassChanges

import static TestDataGenerators.createAlert
import static java.util.Collections.emptyList

@ConfineMetaClassChanges([IqClientFactory])
class IqPolicyEvaluatorTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  def scanResult = Mock(ScanResult)

  def remoteScanResult = Mock(RemoteScanResult)

  def channel = Mock(Channel) {
    call(_ as RemoteScanner) >> remoteScanResult
    call(null) >> remoteScanResult
  }

  def launcher = Mock(Launcher) {
    getChannel() >> channel
  }

  def workspace = new FilePath(new File('/tmp/path'))

  def iqClient = Mock(InternalIqClient)

  def proprietaryConfig = new ProprietaryConfig(['com.example'], ['^org.*'])

  def envVars = new EnvVars(['SCAN_PATTERN': 'some-scan-pattern', 'MODULE_EXCLUDE': 'some-module-exclude'])

  def job = Mock(AbstractProject)

  def run = GroovyMock(AbstractBuild)

  def reportUrl = 'http://server/report'

  def remoteRepositoryUrlFinder = Mock(RemoteRepositoryUrlFinder)

  def repositoryUrl = 'https://a.com/b/c'

  File localScanFile = Mock()

  def setup() {
    def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
    globalConfiguration.iqConfigs = []
    globalConfiguration.iqConfigs.add(new NxiqConfiguration('id', 'internalId', 'displayName',
        'http://server/path', 'credentialsId', false))
    globalConfiguration.save()
    GroovyMock(IqClientFactory, global: true)
    GroovyMock(RemoteScannerFactory, global: true)
    GroovyMock(RemoteRepositoryUrlFinderFactory, global: true)
    iqClient.getLicensedFeatures() >> []
    iqClient.evaluateApplication('appId', 'stage', *_) >> getAnyApplicationPolicyEvaluation()
    IqClientFactory.getIqClient(*_) >> iqClient
    remoteScanResult.copyToLocalScanResult() >> scanResult
    scanResult.scanFile >> localScanFile
    run.getEnvironment(_) >> envVars
    run.parent >> job
    run.workspace >> workspace
    RemoteRepositoryUrlFinderFactory.getRepositoryUrlFinder(workspace, _, _, 'appId', _) >> remoteRepositoryUrlFinder
    channel.call(remoteRepositoryUrlFinder) >> repositoryUrl
  }

  def 'it sets the IQ Instance id to the expected value for the build step'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id1', 'internalId1', 'displayName1', 'serverUrl1',
          'credentialsId1', false))
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id2', 'internalId2', 'displayName2', 'serverUrl2',
          'credentialsId2', false))
      globalConfiguration.save()
      def buildStep = new IqPolicyEvaluatorBuildStep(null, null, null, null, null, null, null, null, null, null, null, null, null)

    when:
      buildStep.setIqInstanceId(iqInstanceId)

    then:
      buildStep.iqInstanceId == expectedIqInstanceId

    where:
      iqInstanceId   | expectedIqInstanceId
      'iqInstanceId' | 'iqInstanceId'
      null           | 'id1'
      ''             | 'id1'
  }
  
  def 'it sets the IQ Instance id to the expected value for the workflow step'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id1', 'internalId1', 'displayName1', 'serverUrl1',
          'credentialsId1', false))
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id2', 'internalId2', 'displayName2', 'serverUrl2',
          'credentialsId2', false))
      globalConfiguration.save()
      def workflowStep = new IqPolicyEvaluatorWorkflowStep(null, null)

    when:
      workflowStep.setIqInstanceId(iqInstanceId)

    then:
      workflowStep.iqInstanceId == expectedIqInstanceId

    where:
      iqInstanceId   | expectedIqInstanceId
      'iqInstanceId' | 'iqInstanceId'
      null           | 'id1'
      ''             | 'id1'
  }

  def 'it retrieves proprietary config followed by remote scan followed by evaluation in correct order (happy path)'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', null, new SelectedApplication('appId'),
          [new ScanPattern('*.jar')], [], false, false, null, null, null, false, null)
      def evaluationResult = getAnyApplicationPolicyEvaluation()
      def remoteScanner = Mock(RemoteScanner)

    when:
      buildStep.perform((AbstractBuild) run, launcher, Mock(BuildListener))

    then: 'retrieves proprietary config'
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.getProprietaryConfigForApplicationEvaluation('appId') >> proprietaryConfig

    then: 'performs a remote scan'
      1 * RemoteScannerFactory.getRemoteScanner('appId', 'stage', ['*.jar'], [], workspace,
          proprietaryConfig, _ as Logger, _, _, _, _) >> remoteScanner
      1 * channel.call(remoteScanner) >> remoteScanResult

    then: 'evaluates the result'
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult, *_) >> evaluationResult
    then: 'delete the temp scan file'
      1 * localScanFile.delete() >> true
  }

  def 'it expands environment variables for scan pattern'() {
    setup:
      def remoteScanner = Mock(RemoteScanner)
      def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', null, new SelectedApplication('appId'),
          [new ScanPattern('/path1/$SCAN_PATTERN/path2/')],
          [], false, false, '131-cred', null, null, false, null)

    when:
      buildStep.perform(run, launcher, Mock(BuildListener))

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * RemoteScannerFactory.
          getRemoteScanner('appId', 'stage', ['/path1/some-scan-pattern/path2/'], _, workspace, _, _ as Logger,
              _, _, _, _) >> remoteScanner
  }

  def 'it ignores when no environment variables set for scan pattern'() {
    setup:
      def remoteScanner = Mock(RemoteScanner)
      def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', null, new SelectedApplication('appId'),
          [new ScanPattern('/path1/$NONEXISTENT_SCAN_PATTERN/path2/')], [], false, false, '131-cred', null, null, false, null)

    when:
      buildStep.perform(run, launcher, Mock(BuildListener))

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * RemoteScannerFactory.
          getRemoteScanner('appId', 'stage', ['/path1/$NONEXISTENT_SCAN_PATTERN/path2/'], _, workspace, _, _ as Logger,
              _, _, _, _) >> remoteScanner
  }

  def 'it passes module excludes to the remote scanner'() {
    setup:
      def remoteScanner = Mock(RemoteScanner)
      def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', null, new SelectedApplication('appId'), [],
          [new ModuleExclude('/path1/$NONEXISTENT_MODULE_EXCLUDE/path2/')], false, false, '131-cred', null, null, false, null)

    when:
      buildStep.perform(run, launcher, Mock(BuildListener))

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
      def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', null, new SelectedApplication('appId'), [],
          [new ModuleExclude('/path1/$MODULE_EXCLUDE/path2/')], false, false, '131-cred', null, null, false, null)

    when:
      buildStep.perform(run, launcher, Mock(BuildListener))

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
      def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', null, new SelectedApplication('appId'), [new ScanPattern('*.jar')], [],
          failBuildOnNetworkError, false, '131-cred', null, null, false, null)

    when:
      buildStep.perform(run, launcher, Mock(BuildListener))

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
          new IOException('CRASH'))   | true                    || IqClientException | 'BOOM!!'
  }

  def 'exception handling (part 2)'() {
    setup:
      iqClient.getProprietaryConfigForApplicationEvaluation('appId') >> { throw exception }
      def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', null, new SelectedApplication('appId'), [new ScanPattern('*.jar')], [],
          failBuildOnNetworkError, false, '131-cred', null, null, false, null)
      PrintStream logger = Mock()
      BuildListener listener = Mock() {
        getLogger() >> logger
      }

    when:
      buildStep.perform(run, launcher, listener)

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      noExceptionThrown()
      1 * run.setResult(Result.UNSTABLE)
      1 * logger.println({String c -> c.startsWith('com.sonatype.nexus.api.exception.IqClientException: BOOM!!')})

    where:
      exception                     | failBuildOnNetworkError || expectedException | expectedMessage
      new IqClientException('BOOM!!',
          new IOException('CRASH')) | false                   || null              | 'BOOM!!'
  }

  def 'it throws an exception when remote scanner fails'() {
    setup:
      iqClient.getProprietaryConfigForApplicationEvaluation('appId') >> proprietaryConfig
      def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', null, new SelectedApplication('appId'), [new ScanPattern('*.jar')], [],
          false, false, '131-cred', null, null, false, null)
      RemoteScanner remoteScanner = Mock()
      RemoteScannerFactory.getRemoteScanner(*_) >> remoteScanner

    when:
      buildStep.perform(run, launcher, Mock(BuildListener))

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * channel.call(remoteScanner) >> { throw new IOException('CRASH') }
      IOException e = thrown()
      e.message == 'CRASH'
    and: 'no need to delete the temp scan file since it was not created'
      0 * localScanFile.delete()
      0 * remoteScanResult.delete()
  }

  def 'evaluation networking exceptions are suppressed by failBuildOnNetworkError'() {
    setup:
      def failBuildOnNetworkError = false
      def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', null, new SelectedApplication('appId'), [new ScanPattern('*.jar')], [],
          failBuildOnNetworkError, false, '131-cred', null, null, false, null)

    when:
      buildStep.perform(run, launcher, Mock(BuildListener))

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult, *_) >>
          { throw new IqClientException('SNAP', new IOException('CRASH')) }
      noExceptionThrown()
    and: 'delete the temp scan file'
      1 * localScanFile.delete() >> true
      1 * remoteScanResult.delete() >> true
  }

  def 'global no credentials are passed to the client builder when no job credentials provided'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', null, new SelectedApplication('appId'),
          [new ScanPattern('*.jar')], [], true, false, jobCredentials, null, null, false, null)

    when:
      buildStep.perform(run, launcher, Mock(BuildListener))

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * IqClientFactory.getIqClient { it.credentialsId == expectedJobCredentials } >> iqClient

    where:
      jobCredentials | expectedJobCredentials
      null           | 'credentialsId'
      ''             | 'credentialsId'
      '131-cred'     | '131-cred'
  }

  def 'evaluation result outcome determines build status'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', null, new SelectedApplication('appId'), [new ScanPattern('*.jar')], [],
          false, false, '131-cred', null, null, false, null)

    when:
      buildStep.perform(run, launcher, Mock(BuildListener))

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult, *_) >> getAnyApplicationPolicyEvaluation(alerts)
      1 * run.setResult(buildResult)
    and: 'delete the temp scan file'
      1 * localScanFile.delete() >> true
      1 * remoteScanResult.delete() >> true

    where:
      alerts                                                  || buildResult
      []                                                      || Result.SUCCESS
      [new PolicyAlert(null, [new Action(Action.ID_WARN)])]   || Result.UNSTABLE
      [new PolicyAlert(null, [new Action(Action.ID_NOTIFY)])] || Result.SUCCESS
  }

  def 'evaluation summary error count determines unstable status'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', null, new SelectedApplication('appId'), [new ScanPattern('*.jar')], [],
          false, false, '131-cred', null, null, false, null)
      def scan = Mock(Scan)
      def summary = Mock(ScanSummary)
      scanResult.scan >> scan
      scan.summary >> summary
      summary.errorCount >> 1

    when:
      buildStep.perform(run, launcher, Mock(BuildListener))

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult, *_) >> getAnyApplicationPolicyEvaluation(alerts)
      1 * run.setResult(buildResult)
    and: 'delete the temp scan file'
      1 * localScanFile.delete() >> true
      1 * remoteScanResult.delete() >> true

    where:
      alerts                                                  || buildResult
      []                                                      || Result.UNSTABLE
      [new PolicyAlert(null, [new Action(Action.ID_NOTIFY)])] || Result.UNSTABLE
  }

  def 'Exception thrown for errors when failForScanningErrors true'() {
    setup:
    def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', null, new SelectedApplication('appId'),
        [new ScanPattern('*.jar')], [], false, true, '131-cred', null, null, false, null)
    def scan = Mock(Scan)
    def summary = Mock(ScanSummary)
    summary.errorCount >> 1
    scan.summary >> summary
    scanResult.scan >> scan

    when:
    buildStep.perform(run, launcher, Mock(BuildListener))

    then:
    1 * iqClient.verifyOrCreateApplication(*_) >> true
    1 * iqClient.evaluateApplication('appId', 'stage', scanResult, *_) >> getAnyApplicationPolicyEvaluation()
    and:
    PolicyEvaluationException ex = thrown()
    ex.message == 'IQ Server evaluation of application appId failed'

    and: 'delete the temp scan file'
    1 * localScanFile.delete() >> true
    1 * remoteScanResult.delete() >> true
  }

  def 'build unstable when scanning errors but failOnScanningErrors is false'() {
    setup:
    def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', null, new SelectedApplication('appId'),
        [new ScanPattern('*.jar')], [], false, false, '131-cred', null, null, false, null)
    def scan = Mock(Scan)
    def summary = Mock(ScanSummary)
    summary.errorCount >> 1
    scan.summary >> summary
    scanResult.scan >> scan

    when:
    buildStep.perform(run, launcher, Mock(BuildListener))

    then:
    1 * iqClient.verifyOrCreateApplication(*_) >> true
    1 * iqClient.evaluateApplication('appId', 'stage', scanResult, *_) >> getAnyApplicationPolicyEvaluation()
    1 * run.setResult(Result.UNSTABLE)

    and: 'delete the temp scan file'
    1 * localScanFile.delete() >> true
    1 * remoteScanResult.delete() >> true
  }

  def 'Exception not thrown when failForScanningErrors true but no errors'() {
    setup:
    def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', null, new SelectedApplication('appId'),
        [new ScanPattern('*.jar')], [], false, true, '131-cred', null, null, false, null)
    def scan = Mock(Scan)
    def summary = Mock(ScanSummary)
    summary.errorCount >> 0
    scan.summary >> summary
    scanResult.scan >> scan

    when:
    buildStep.perform(run, launcher, Mock(BuildListener))

    then:
    1 * iqClient.verifyOrCreateApplication(*_) >> true
    1 * iqClient.evaluateApplication('appId', 'stage', scanResult, *_) >> getAnyApplicationPolicyEvaluation()
    1 * run.setResult(Result.SUCCESS)

    and: 'delete the temp scan file'
    1 * localScanFile.delete() >> true
    1 * remoteScanResult.delete() >> true
  }


  def 'evaluation throws exception when build results in failure'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', null, new SelectedApplication('appId'), [new ScanPattern('*.jar')], [],
          false, false, '131-cred', null, null, false, null)
      def policyEvaluation = getAnyApplicationPolicyEvaluation([new PolicyAlert(null, [new Action(Action.ID_FAIL)])])

    when:
      buildStep.perform(run, launcher, Mock(BuildListener))

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult, *_) >> policyEvaluation
      1 * run.setResult(Result.FAILURE)

    and:
      PolicyEvaluationException ex = thrown()
      ex.message == 'IQ Server evaluation of application appId failed'
      ex.policyEvaluation == policyEvaluation
    and: 'delete the temp scan file'
      1 * localScanFile.delete() >> true
      1 * remoteScanResult.delete() >> true
  }

  def 'evaluation throws exception when build results in failure with error count'() {
    setup:
      def scan = Mock(Scan)
      def summary = Mock(ScanSummary)
      scanResult.scan >> scan
      scan.summary >> summary
      summary.errorCount >> 1

      def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', null, new SelectedApplication('appId'), [new ScanPattern('*.jar')], [],
          false, false, '131-cred', null, null, false, null)
      def policyEvaluation = getAnyApplicationPolicyEvaluation([new PolicyAlert(null, [new Action(Action.ID_FAIL)])])

    when:
      buildStep.perform(run, launcher, Mock(BuildListener))

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult, *_) >> policyEvaluation
      1 * run.setResult(Result.FAILURE)
      0 * run.setResult(Result.UNSTABLE)

    and:
      PolicyEvaluationException ex = thrown()
      ex.message == 'IQ Server evaluation of application appId failed'
      ex.policyEvaluation == policyEvaluation
    and: 'delete the temp scan file'
      1 * localScanFile.delete() >> true
      1 * remoteScanResult.delete() >> true
  }

  @Unroll
  def 'prints an error message on failure when configured with hideReports = #hideReports'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', null, new SelectedApplication('appId'), [new ScanPattern('*.jar')], [],
          false, false, '131-cred', null, null, false, null)
      BuildListener listener = Mock()
      PrintStream log = Mock()
      listener.getLogger() >> log
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id', 'internalId', 'displayName',
          'http://server/path', 'credentialsId', hideReports))
      globalConfiguration.save()
      def trigger = createAlert(Action.ID_FAIL).trigger

    when:
      buildStep.perform(run, launcher, listener)

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult, *_) >>
          getAnyApplicationPolicyEvaluation([new PolicyAlert(trigger, [new Action(Action.ID_FAIL)])], 11, 12, 13)
    and:
      thrown PolicyEvaluationException
      (hideReports ? 0 : 1) * log.println(
          'Nexus IQ reports policy failing due to \nPolicy(policyName) [\n Component(displayName=value, ' +
              'hash=12hash34) [\n  Constraint(constraintName) [summary because: reason] ]]\nThe detailed report can be' +
              ' viewed online at http://server/report\nSummary of policy violations: 11 critical, 12 severe, 13 moderate')
      1 * listener.fatalError('IQ Server evaluation of application appId failed')
    and: 'delete the temp scan file'
      1 * localScanFile.delete() >> true
      1 * remoteScanResult.delete() >> true

    where:
      hideReports << [true, false]
  }

  @Unroll
  def 'prints a log message on warnings when configured with hideReports = #hideReports'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', null, new SelectedApplication('appId'), [new ScanPattern('*.jar')], [],
          false, false, '131-cred', null, null, false, null)
      BuildListener listener = Mock()
      PrintStream log = Mock()
      listener.getLogger() >> log
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id', 'internalId', 'displayName',
          'http://server/path', 'credentialsId', hideReports))
      globalConfiguration.save()
      def trigger = createAlert(Action.ID_FAIL).trigger

    when:
      buildStep.perform(run, launcher, listener)

    then:
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult, *_) >> getAnyApplicationPolicyEvaluation([new PolicyAlert(trigger, [new Action(Action.ID_WARN)])], 11, 12, 13)
      1 * log.println('IQ Server evaluation of application appId detected warnings')
      (hideReports ? 0 : 1) * log.println(
          'Nexus IQ reports policy warning due to \nPolicy(policyName) [\n Component(displayName=value, ' +
              'hash=12hash34) [\n  Constraint(constraintName) [summary because: reason] ]]\nThe detailed report can be' +
              ' viewed online at http://server/report\nSummary of policy violations: 11 critical, 12 severe, 13 moderate')
    and: 'delete the temp scan file'
      1 * localScanFile.delete() >> true
      1 * remoteScanResult.delete() >> true

    where:
      hideReports << [true, false]
  }

  @Unroll
  def 'prints a summary on success [App Id: #appId, Org Id: #orgId]'() {
    given: 'a configured build step'
      def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', orgId, new SelectedApplication(appId), [new ScanPattern('*.jar')], [],
          false, false, '131-cred', null, null, false, null)
      BuildListener listener = Mock()
      PrintStream log = Mock()
      listener.getLogger() >> log

    when: 'the build step is executed'
      buildStep.perform(run, launcher, listener)

    then: 'the summary is printed'
      1 * iqClient.verifyOrCreateApplication(appId, orgId) >> true
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult, *_) >> getAnyApplicationPolicyEvaluation()
      0 * log.println('WARNING: IQ Server evaluation of application appId detected warnings.')
      1 * log.println('\nThe detailed report can be viewed online at http://server/report\n' +
          'Summary of policy violations: 0 critical, 0 severe, 0 moderate')

    and: 'delete the temp scan file'
      1 * localScanFile.delete() >> true
      1 * remoteScanResult.delete() >> true

    where: 'the app id is #appId, and the org id is #orgId'
      appId   | orgId
      'appId' | null
      'appId' | 'orgId'
  }

  @Unroll
  def 'prints proper error message when app verification fails [Org Id: #orgId]'() {
    given: 'a configured build step'
      def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', orgId, new SelectedApplication('appId'), [new ScanPattern('*.jar')], [],
          false, false, '131-cred', null, null, false, null)
      BuildListener listener = Mock()
      PrintStream log = Mock()
      listener.getLogger() >> log

    when: 'the build step is executed'
      buildStep.perform(run, launcher, listener)

    then: 'the app verification fails'
      1 * iqClient.verifyOrCreateApplication('appId', orgId) >> false

    and: 'the proper error is thrown'
      def e = thrown IllegalArgumentException
      e.message == expectedMessage

    where: 'the app id is #orgId and the expected message #expectedMessage'
      orgId   | expectedMessage
      null    | 'The application ID appId is invalid.'
      'orgId' | 'The application ID appId is invalid for organization ID orgId.'
  }

  def 'prints an error message if not in node context'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', null, new SelectedApplication('appId'),
          [new ScanPattern('*.jar')], [],false, false, null, null, null, false, null)
      def listener = Mock(BuildListener)

    when:
      buildStep.perform(run, null, listener)

    then:
      thrown AbortException
      1 * run.setResult(Result.FAILURE)
      1 * listener.error('nexusPolicyEvaluation step requires a node context. Please specify an agent or a node block')
  }

  def 'it adds or updates source control with the retrieved repo url'() {
    setup:
      def buildStep = new IqPolicyEvaluatorBuildStep(null, 'stage', null, new SelectedApplication('appId'),
          [new ScanPattern('*.jar')], [],
          false, false, null, null, null, false, null)
      def evaluationResult = getAnyApplicationPolicyEvaluation()
      def remoteScanner = Mock(RemoteScanner)

    when:
      buildStep.perform((AbstractBuild) run, launcher, Mock(BuildListener))

    then: 'retrieves proprietary config'
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.getProprietaryConfigForApplicationEvaluation('appId') >> proprietaryConfig

    then: 'performs a remote scan'
      1 * RemoteScannerFactory.getRemoteScanner('appId', 'stage', ['*.jar'], [], workspace,
          proprietaryConfig, _ as Logger, _, _, _, _) >> remoteScanner
      1 * channel.call(remoteScanner) >> remoteScanResult

    then: 'performs a remote scan'
      1 * RemoteRepositoryUrlFinderFactory.getRepositoryUrlFinder(workspace, _, _, 'appId', _) >>
          remoteRepositoryUrlFinder
      1 * channel.call(remoteRepositoryUrlFinder) >> repositoryUrl
      1 * iqClient.addOrUpdateSourceControl('appId', repositoryUrl, _)

    then: 'evaluates the result'
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult, *_) >> evaluationResult
    and: 'delete the temp scan file'
      1 * localScanFile.delete() >> true
      1 * remoteScanResult.delete() >> true
  }

  def 'evaluates the application with default callflow options when callflow is enabled but no additional options are sent'() {
    setup:
      def evaluationResult = getAnyApplicationPolicyEvaluation()

      def pathReturnedByExpandingIqScanPatterns = new File("some-file.jar")
      def defaultCallflowOptions = new CallflowOptions(
          [pathReturnedByExpandingIqScanPatterns.getAbsolutePath()],
          null,
          null
      )

      def remoteScanner = Mock(RemoteScanner)
      iqClient.verifyOrCreateApplication(*_) >> true
      iqClient.getProprietaryConfigForApplicationEvaluation('appId') >> proprietaryConfig
      RemoteScannerFactory.getRemoteScanner(*_) >> remoteScanner
      channel.call(_) >> remoteScanResult

    when:
      getBuildStepForCallflowTests(true, null)
          .perform((AbstractBuild) run, launcher, Mock(BuildListener))

    then: 'evaluates the results using default callflow options'
      1 * remoteScanner.getScanTargets(_, _) >> [pathReturnedByExpandingIqScanPatterns]
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult, _, {
        it.scanTargets == defaultCallflowOptions.scanTargets &&
            it.namespaces == null &&
            it.additionalConfiguration == null
      } as CallflowOptions) >> evaluationResult
  }

  def 'evaluates the application with additional callflow options when callflow is enabled and additional options are sent'() {
    setup:
      def expectedNamespaces = ['any.namespace']
      def givenAdditionalConfig =  [some: "property"]
      def expectedScanTargets = [
          new File("some-path-1").getAbsolutePath(),
          new File('some-path-2').getAbsolutePath()
      ]
      def expectedProps =  new Properties().with {
        it.put("some", "property")
        it
      }

      def evaluationResult = getAnyApplicationPolicyEvaluation()

      def remoteScanner = Mock(RemoteScanner)
      iqClient.verifyOrCreateApplication(*_) >> true
      iqClient.getProprietaryConfigForApplicationEvaluation('appId') >> proprietaryConfig
      RemoteScannerFactory.getRemoteScanner(*_) >> remoteScanner
      channel.call(_) >> remoteScanResult

    when:
      getBuildStepForCallflowTests(
          true,
          new CallflowConfiguration(
              [new ScanPattern("/some-path/**/*.jar")],
              expectedNamespaces,
              givenAdditionalConfig)
      ).perform((AbstractBuild) run, launcher, Mock(BuildListener))

    then: 'evaluates the results using default callflow options'
      1 * remoteScanner.getScanTargets(*_) >> [new File("some-path-1"), new File('some-path-2')]
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult, _, {
        it.scanTargets == expectedScanTargets &&
            it.namespaces == expectedNamespaces &&
            it.additionalConfiguration == expectedProps
      } as CallflowOptions) >> evaluationResult
  }

  def 'evaluates the application with null for callflow options when callflow is disabled'() {
    setup:
      def evaluationResult = getAnyApplicationPolicyEvaluation()

      def remoteScanner = Mock(RemoteScanner)
      iqClient.verifyOrCreateApplication(*_) >> true
      iqClient.getProprietaryConfigForApplicationEvaluation('appId') >> proprietaryConfig
      RemoteScannerFactory.getRemoteScanner(*_) >> remoteScanner
      channel.call(_) >> remoteScanResult

    when:
      getBuildStepForCallflowTests(false, null)
          .perform((AbstractBuild) run, launcher, Mock(BuildListener))

    then: 'evaluates the results using default callflow options'
      1 * iqClient.evaluateApplication('appId', 'stage', scanResult, _, null) >> evaluationResult
  }

  private ApplicationPolicyEvaluation getAnyApplicationPolicyEvaluation(
      List<PolicyAlert> alerts = emptyList(),
      int criticalPolicyViolationCount = 0,
      int severePolicyViolationCount = 0,
      int moderatePolicyViolationCount = 0
  ) {
    return new ApplicationPolicyEvaluation(
        0,
        0,
        0,
        0,
        criticalPolicyViolationCount,
        severePolicyViolationCount,
        moderatePolicyViolationCount,
        0,
        0,
        alerts,
        reportUrl
    )
  }

  private IqPolicyEvaluatorBuildStep getBuildStepForCallflowTests(
      final Boolean runCallflow,
      final CallflowConfiguration callflowConfiguration
  ) {
    return new IqPolicyEvaluatorBuildStep(
        null,
        'stage',
        null,
        new SelectedApplication('appId'),
        [new ScanPattern('*.jar')],
        [],
        false,
        false,
        null,
        null,
        null,
        runCallflow,
        callflowConfiguration
    )
  }
}
