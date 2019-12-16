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


import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.NxiqConfiguration

import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import hudson.model.FreeStyleProject
import hudson.model.Result
import hudson.model.Slave
import hudson.slaves.EnvironmentVariablesNodeProperty
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Rule
import org.jvnet.hudson.test.ExtractResourceSCM
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat
import static com.github.tomakehurst.wiremock.client.WireMock.okJson
import static com.github.tomakehurst.wiremock.client.WireMock.post
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

/**
 * Test builds on slave using WireMock. A HTTP Mock service is required over Spock's GroovyMock as the slave runs
 * under a separate JVM and therefore the IQ client cannot be mocked. IQ client behavior should be tested in its
 * project and these tests should just verify RemoteScanner works on a slave.
 */
class IqPolicyEvaluatorSlaveIntegrationTest
    extends Specification
    implements ClassFilterLoggingTestTrait
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort())

  /**
   * Minimal server mock to allow plugin to succeed
   */
  def configureIqServerMock(serverVersion = IqPolicyEvaluatorUtil.MINIMAL_SERVER_VERSION_REQUIRED) {
    WireMock.configureFor("localhost", wireMockRule.port())
    givenThat(get(urlMatching('/rest/config/proprietary\\?.*'))
        .willReturn(okJson('{}')))
    givenThat(post(urlMatching('/rest/integration/applications/verifyOrCreate/.*'))
        .willReturn(okJson('true')))
    givenThat(post(urlMatching('/rest/integration/applications/app/evaluations/ci/stages/.*'))
        .willReturn(okJson('{"statusId": "statusId"}')))
    givenThat(get(urlMatching('/rest/integration/applications/app/evaluations/status/statusId'))
        .willReturn(okJson('''{
        "status": "COMPLETED",
        "reason": null,
        "result": {
        "alerts": [],
        "affectedComponentCount": 33,
        "criticalComponentCount": 20,
        "severeComponentCount": 12,
        "moderateComponentCount": 1,
        "criticalPolicyViolationCount": 46,
        "severePolicyViolationCount": 54,
        "moderatePolicyViolationCount": 3,
        "grandfatheredPolicyViolationCount": 0
        },
        "scanReceipt": {
        "scanId": "scanId",
        "timeToReport": 6,
        "reportUrl": "ui/links/application/app/report/scanId",
        "pdfUrl": "ui/links/application/app/report/scanId/pdf",
        "dataUrl": "api/v2/applications/app/reports/scanId/raw",
        "reportTimeoutInSeconds": 350
        },
        "nextPollingIntervalInSeconds": 0
        }''')))
    givenThat(get(urlMatching('/rest/product/version'))
        .willReturn(okJson("""{"tag": "1e64d778447fc30e4f509f9ca965c5bbe7aa8fd3",
        "version": "${serverVersion}",
        "name": "sonatype-clm-server",
        "timestamp": "201807111516",
        "build": "build-number"}""")))
    givenThat(post(urlMatching('/api/v2/sourceControl.*'))
        .willReturn(okJson("""{"id": "id",
            "ownerId" : "",
            "repositoryUrl": "",
            "token": "",
            "provider": ""
            }""")))
  }

  def configureJenkins() {
    def nxiqConfiguration = [new NxiqConfiguration("http://localhost:${wireMockRule.port()}", 'cred-id')]
    GlobalNexusConfiguration.globalNexusConfiguration.iqConfigs = nxiqConfiguration
    GlobalNexusConfiguration.globalNexusConfiguration.nxrmConfigs = []
    def credentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, 'cred-id', 'name', 'user',
        'password')
    CredentialsProvider.lookupStores(jenkins.jenkins).first().addCredentials(Domain.global(), credentials)
  }

  def 'Should perform a freestyle build on slave'() {
    given: 'a jenkins project'
      FreeStyleProject project = jenkins.createFreeStyleProject()
      project.assignedNode = jenkins.createSlave()
      project.buildersList.
          add(new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('app'), [], [], false, 'cred-id', null))
      configureJenkins()

    and: 'a mock IQ server stub'
      configureIqServerMock()

    when: 'the build is scheduled'
      def build = project.scheduleBuild2(0).get()

    then: 'the return code is successful'
      jenkins.assertBuildStatusSuccess(build)
  }

  def 'Should perform a freestyle build on slave with new server version'() {
    given: 'a jenkins project'
      FreeStyleProject project = jenkins.createFreeStyleProject()
      project.assignedNode = jenkins.createSlave()
      project.buildersList.
          add(new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('app'), [], [], false, 'cred-id', null))
      configureJenkins()

    and: 'a mock IQ server stub'
      configureIqServerMock(incrementVersion(IqPolicyEvaluatorUtil.MINIMAL_SERVER_VERSION_REQUIRED))

    when: 'the build is scheduled'
      def build = project.scheduleBuild2(0).get()

    then: 'the return code is successful'
      jenkins.assertBuildStatusSuccess(build)
  }

  def 'Should not perform a freestyle build on slave with older server version'() {
    given: 'a jenkins project'
      FreeStyleProject project = jenkins.createFreeStyleProject()
      project.assignedNode = jenkins.createSlave()
      project.buildersList.
          add(new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('app'), [], [], false, 'cred-id', null))
      configureJenkins()

    and: 'a mock IQ server stub'
      configureIqServerMock(decrementVersion(IqPolicyEvaluatorUtil.MINIMAL_SERVER_VERSION_REQUIRED))

    when: 'the build is scheduled'
      def build = project.scheduleBuild2(0).get()

    then: 'the return code is failure'
      jenkins.assertBuildStatus(Result.FAILURE, build)
  }

  def 'Should perform a pipeline build on slave'() {
    given: 'a jenkins project'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      Slave slave = jenkins.createSlave()
      configureJenkins()

    and: 'a mock IQ server stub'
      configureIqServerMock()

    when: 'the build is scheduled'
      project.definition = new CpsFlowDefinition("""node ('${slave.getNodeName()}') {
          writeFile file: 'dummy.txt', text: 'dummy'
          nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: 'app', iqStage: 'stage'
          }\n""")
      def build = project.scheduleBuild2(0).get()

    then: 'the return code is successful'
      jenkins.assertBuildStatusSuccess(build)
  }

  def 'Should perform a pipeline build on slave with newer server version'() {
    given: 'a jenkins project'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      Slave slave = jenkins.createSlave()
      configureJenkins()

    and: 'a mock IQ server stub'
      configureIqServerMock(incrementVersion(IqPolicyEvaluatorUtil.MINIMAL_SERVER_VERSION_REQUIRED))

    when: 'the build is scheduled'
      project.definition = new CpsFlowDefinition("""node ('${slave.getNodeName()}') {
          writeFile file: 'dummy.txt', text: 'dummy'
          nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: 'app', iqStage: 'stage'
          }\n""")
      def build = project.scheduleBuild2(0).get()

    then: 'the return code is successful'
      jenkins.assertBuildStatusSuccess(build)
  }

  def 'Should not perform a pipeline build on slave with older server version'() {
    given: 'a jenkins project'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      Slave slave = jenkins.createSlave()
      configureJenkins()

    and: 'a mock IQ server stub'
      configureIqServerMock(decrementVersion(IqPolicyEvaluatorUtil.MINIMAL_SERVER_VERSION_REQUIRED))

    when: 'the build is scheduled'
      project.definition = new CpsFlowDefinition("""node ('${slave.getNodeName()}') {
          writeFile file: 'dummy.txt', text: 'dummy'
          nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: 'app', iqStage: 'stage'
          }\n""")
      def build = project.scheduleBuild2(0).get()

    then: 'the return code is failure'
      jenkins.assertBuildStatus(Result.FAILURE, build)
  }

  def 'Freestyle build with repo env var should call addOrUpdateSourceControl'() {
    given: 'a jenkins project'
      def url = 'http://a.com/b/c'
      def prop = new EnvironmentVariablesNodeProperty()
      def env = prop.getEnvVars()
      env.put('GIT_URL', url)
      jenkins.jenkins.globalNodeProperties.add(prop)
      FreeStyleProject project = jenkins.createFreeStyleProject()
      project.assignedNode = jenkins.createSlave()
      project.buildersList.
          add(new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('app'), [], [], false, 'cred-id', null))
      configureJenkins()

    and: 'a mock IQ server stub'
      configureIqServerMock(incrementVersion(IqPolicyEvaluatorUtil.MINIMAL_SERVER_VERSION_REQUIRED))

    when: 'the build is scheduled'
      def build = project.scheduleBuild2(0).get()

    then: 'the source control onboarding is called with the repo url'
      jenkins.assertBuildStatusSuccess(build)
      assert wireMockRule.countRequestsMatching(
          RequestPatternBuilder.newRequestPattern(RequestMethod.POST, urlMatching('/api/v2/sourceControl.*'))
              .build()).count == 1
  }

  def 'Freestyle build within git context should call addOrUpdateSourceControl'() {
    given: 'a jenkins project'
      FreeStyleProject project = jenkins.createFreeStyleProject()
      project.assignedNode = jenkins.createSlave()
      def path = getClass().getResource('sampleRepoWithRemoteUrl.zip')
      project.setScm(new ExtractResourceSCM(path))
      project.buildersList
          .add(new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('app'), [], [], false, 'cred-id', null))
      configureJenkins()

    and: 'a mock IQ server stub'
      configureIqServerMock(incrementVersion(IqPolicyEvaluatorUtil.MINIMAL_SERVER_VERSION_REQUIRED))

    when: 'the build is scheduled'
      def build = project.scheduleBuild2(0).get()

    then: 'the source control onboarding is called with the repo url'
      jenkins.assertBuildStatusSuccess(build)
      assert wireMockRule.countRequestsMatching(
          RequestPatternBuilder.newRequestPattern(RequestMethod.POST, urlMatching('/api/v2/sourceControl.*'))
              .build()).count == 1
  }

  def 'Pipeline build with repo env var should call addOrUpdateSourceControl'() {
    given: 'a jenkins project'
      def url = 'http://a.com/b/c'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      Slave slave = jenkins.createSlave()
      configureJenkins()

    and: 'a mock IQ server stub'
      configureIqServerMock(incrementVersion(IqPolicyEvaluatorUtil.MINIMAL_SERVER_VERSION_REQUIRED))

    when: 'the nexus policy evaluator is executed'
      project.definition = new CpsFlowDefinition("node ('${slave.getNodeName()}') {\n" +
          "withEnv(['GIT_URL=" + url + "']) {\n" +
          'writeFile file: \'dummy.txt\', text: \'dummy\'\n' +
          "nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: \'app\', " +
          'iqStage: \'stage\'\n' +
          '}\n' +
          '}\n')
      def build = project.scheduleBuild2(0).get()

    then: 'the source control onboarding is called with the repo url'
      jenkins.assertBuildStatusSuccess(build)
      assert wireMockRule.countRequestsMatching(
          RequestPatternBuilder.newRequestPattern(RequestMethod.POST, urlMatching('/api/v2/sourceControl.*'))
              .build()).count == 1
  }

  def 'Pipeline build within git context should call addOrUpdateSourceControl'() {
    given: 'a jenkins project'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      Slave slave = jenkins.createSlave()
      configureJenkins()

    and: 'a mock IQ server stub'
      configureIqServerMock(incrementVersion(IqPolicyEvaluatorUtil.MINIMAL_SERVER_VERSION_REQUIRED))

    when: 'the nexus policy evaluator is executed'
      def path = new File(getClass().getResource('sampleRepoWithRemoteUrl.zip').toURI()).absolutePath
      project.definition = new CpsFlowDefinition("node ('${slave.getNodeName()}') {\n" +
          'writeFile file: \'dummy.txt\', text: \'dummy\'\n' +
          "unzip zipFile: '" + path + "', glob: '**/*'\n" +
          "nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: \'app\', " +
          'iqStage: \'stage\'\n' +
          '}\n')
      def build = project.scheduleBuild2(0).get()

    then: 'the source control onboarding is called with the repo url'
      jenkins.assertBuildStatusSuccess(build)
      assert wireMockRule.countRequestsMatching(
          RequestPatternBuilder.newRequestPattern(RequestMethod.POST, urlMatching('/api/v2/sourceControl.*'))
              .build()).count == 1
  }

  private String decrementVersion(String version) {
    int dotAt = version.indexOf('.')
    if (dotAt > 0) {
      return (Integer.valueOf(version.substring(0, dotAt)) - 1) + '.' + version.substring(dotAt + 1)
    }
    return String.valueOf(Integer.valueOf(version) - 1)
  }

  private String incrementVersion(String version) {
    int dotAt = version.indexOf('.')
    if (dotAt > 0) {
      return (Integer.valueOf(version.substring(0, dotAt)) + 1) + '.' + version.substring(dotAt + 1)
    }
    return String.valueOf(Integer.valueOf(version) + 1)
  }
}
