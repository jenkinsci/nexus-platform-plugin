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

import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import static org.hamcrest.CoreMatchers.hasItems
import static org.hamcrest.Matchers.not
import static org.junit.Assert.assertThat
import static org.sonatype.nexus.ci.iq.IqServerMockUtility.configureIqServerMock
import static org.sonatype.nexus.ci.iq.IqServerMockUtility.configureJenkins

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

  def 'Should perform a freestyle build on slave'() {
    given: 'a jenkins project'
      FreeStyleProject project = jenkins.createFreeStyleProject()
      project.assignedNode = jenkins.createSlave()
      project.buildersList.
          add(new IqPolicyEvaluatorBuildStep('id', 'stage', null, new SelectedApplication('app'), [], [], false, 'cred-id',
              null, null))
      configureJenkins(jenkins.jenkins, wireMockRule.port())

    and: 'a mock IQ server stub'
      configureIqServerMock(wireMockRule.port())

    when: 'the build is scheduled'
      def build = project.scheduleBuild2(0).get()

    then: 'the return code is successful'
      jenkins.assertBuildStatusSuccess(build)
    
    and: 'two IQ build actions are associated'
      assertThat(build.actions.collect { it.getClass() },
          hasItems(PolicyEvaluationHealthAction.class, PolicyEvaluationReportAction.class))
  }

  def 'Should perform a freestyle build on slave with new server version'() {
    given: 'a jenkins project'
      FreeStyleProject project = jenkins.createFreeStyleProject()
      project.assignedNode = jenkins.createSlave()
      project.buildersList.
          add(new IqPolicyEvaluatorBuildStep('id', 'stage', null, new SelectedApplication('app'), [], [], false, 'cred-id',
              null, null))
      configureJenkins(jenkins.jenkins, wireMockRule.port())

    and: 'a mock IQ server stub'
      configureIqServerMock(wireMockRule.port(), incrementVersion(IqPolicyEvaluatorUtil.MINIMAL_SERVER_VERSION_REQUIRED))

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
          add(new IqPolicyEvaluatorBuildStep('id', 'stage', null, new SelectedApplication('app'), [], [], false, 'cred-id',
              null, null))
      configureJenkins(jenkins.jenkins, wireMockRule.port())

    and: 'a mock IQ server stub'
      configureIqServerMock(wireMockRule.port(), decrementVersion(IqPolicyEvaluatorUtil.MINIMAL_SERVER_VERSION_REQUIRED))

    when: 'the build is scheduled'
      def build = project.scheduleBuild2(0).get()

    then: 'the return code is failure'
      jenkins.assertBuildStatus(Result.FAILURE, build)
  }

  def 'Should perform a pipeline build on slave'() {
    given: 'a jenkins project'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      Slave slave = jenkins.createSlave()
      configureJenkins(jenkins.jenkins, wireMockRule.port())

    and: 'a mock IQ server stub'
      configureIqServerMock(wireMockRule.port())

    when: 'the build is scheduled'
      project.definition = new CpsFlowDefinition("""node ('${slave.getNodeName()}') {
          writeFile file: 'dummy.txt', text: 'dummy'
          nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: 'app', iqStage: 'stage'
          }\n""", false)
      def build = project.scheduleBuild2(0).get()

    then: 'the return code is successful'
      jenkins.assertBuildStatusSuccess(build)
  }

  def 'Should perform a pipeline build on slave with newer server version'() {
    given: 'a jenkins project'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      Slave slave = jenkins.createSlave()
      configureJenkins(jenkins.jenkins, wireMockRule.port())

    and: 'a mock IQ server stub'
      configureIqServerMock(wireMockRule.port(), incrementVersion(IqPolicyEvaluatorUtil.MINIMAL_SERVER_VERSION_REQUIRED))

    when: 'the build is scheduled'
      project.definition = new CpsFlowDefinition("""node ('${slave.getNodeName()}') {
          writeFile file: 'dummy.txt', text: 'dummy'
          nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: 'app', iqStage: 'stage'
          }\n""", false)
      def build = project.scheduleBuild2(0).get()

    then: 'the return code is successful'
      jenkins.assertBuildStatusSuccess(build)
  }

  def 'Should not perform a pipeline build on slave with older server version'() {
    given: 'a jenkins project'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      Slave slave = jenkins.createSlave()
      configureJenkins(jenkins.jenkins, wireMockRule.port())

    and: 'a mock IQ server stub'
      configureIqServerMock(wireMockRule.port(), decrementVersion(IqPolicyEvaluatorUtil.MINIMAL_SERVER_VERSION_REQUIRED))

    when: 'the build is scheduled'
      project.definition = new CpsFlowDefinition("""node ('${slave.getNodeName()}') {
          writeFile file: 'dummy.txt', text: 'dummy'
          nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: 'app', iqStage: 'stage'
          }\n""", false)
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
          add(new IqPolicyEvaluatorBuildStep('id', 'stage', null, new SelectedApplication('app'), [], [], false, 'cred-id',
              null, null))
      configureJenkins(jenkins.jenkins, wireMockRule.port())

    and: 'a mock IQ server stub'
      configureIqServerMock(wireMockRule.port(), incrementVersion(IqPolicyEvaluatorUtil.MINIMAL_SERVER_VERSION_REQUIRED))

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
      project.buildersList.
          add(new IqPolicyEvaluatorBuildStep('id', 'stage', null, new SelectedApplication('app'), [], [], false, 'cred-id',
              null, null))
      configureJenkins(jenkins.jenkins, wireMockRule.port())

    and: 'a mock IQ server stub'
      configureIqServerMock(wireMockRule.port(), incrementVersion(IqPolicyEvaluatorUtil.MINIMAL_SERVER_VERSION_REQUIRED))

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
      configureJenkins(jenkins.jenkins, wireMockRule.port())

    and: 'a mock IQ server stub'
      configureIqServerMock(wireMockRule.port(), incrementVersion(IqPolicyEvaluatorUtil.MINIMAL_SERVER_VERSION_REQUIRED))

    when: 'the nexus policy evaluator is executed'
      project.definition = new CpsFlowDefinition("node ('${slave.getNodeName()}') {\n" +
          "withEnv(['GIT_URL=" + url + "']) {\n" +
          'writeFile file: \'dummy.txt\', text: \'dummy\'\n' +
          "nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: \'app\', " +
          'iqStage: \'stage\'\n' +
          '}\n' +
          '}\n', false)
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
      configureJenkins(jenkins.jenkins, wireMockRule.port())

    and: 'a mock IQ server stub'
      configureIqServerMock(wireMockRule.port(), incrementVersion(IqPolicyEvaluatorUtil.MINIMAL_SERVER_VERSION_REQUIRED))

    when: 'the nexus policy evaluator is executed'
      def path = new File(getClass().getResource('sampleRepoWithRemoteUrl.zip').toURI()).absolutePath
      project.definition = new CpsFlowDefinition("node ('${slave.getNodeName()}') {\n" +
          'writeFile file: \'dummy.txt\', text: \'dummy\'\n' +
          "unzip zipFile: '" + path.replace('\\', '/') + "', glob: '**/*'\n" +
          "nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: \'app\', " +
          'iqStage: \'stage\'\n' +
          '}\n', false)
      def build = project.scheduleBuild2(0).get()

    then: 'the source control onboarding is called with the repo url'
      jenkins.assertBuildStatusSuccess(build)
      assert wireMockRule.countRequestsMatching(
          RequestPatternBuilder.newRequestPattern(RequestMethod.POST, urlMatching('/api/v2/sourceControl.*'))
              .build()).count == 1
  }
  
  def 'Build with hideReports configured should not include a report action'() {
    given: 'a jenkins project, and "hideReports" feature turned on'
      FreeStyleProject project = jenkins.createFreeStyleProject()
      project.assignedNode = jenkins.createSlave()
      project.buildersList.
          add(new IqPolicyEvaluatorBuildStep('id', 'stage', null, new SelectedApplication('app'), [], [], false, 'cred-id',
              null, null))
      configureJenkins(jenkins.jenkins, wireMockRule.port(), true)

    and: 'a mock IQ server stub'
      configureIqServerMock(wireMockRule.port())

    when: 'the build is scheduled'
      def build = project.scheduleBuild2(0).get()

    then: 'the return code is successful'
      jenkins.assertBuildStatusSuccess(build)

    and: 'only the health IQ build action is associated'
      def actionClasses = build.actions.collect { it.getClass() }
      assertThat(actionClasses, hasItems(PolicyEvaluationHealthAction.class))
      assertThat(actionClasses, not(hasItems(PolicyEvaluationReportAction.class)))
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
