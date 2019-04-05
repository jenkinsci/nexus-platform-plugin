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
package org.sonatype.nexus.ci.nxrm.v3

import com.sonatype.nexus.api.exception.RepositoryManagerException
import com.sonatype.nexus.api.repository.v3.ComponentInfo
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.Nxrm2Configuration
import org.sonatype.nexus.ci.config.Nxrm3Configuration
import org.sonatype.nexus.ci.config.NxrmConfiguration
import org.sonatype.nexus.ci.nxrm.v3.AssociateTagStep.DescriptorImpl
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import hudson.model.Result
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification
import spock.lang.Unroll

import static hudson.model.Result.SUCCESS
import static org.sonatype.nexus.ci.util.FormUtil.EMPTY_LIST_BOX_NAME
import static org.sonatype.nexus.ci.util.FormUtil.EMPTY_LIST_BOX_VALUE

class AssociateTagStepTest
    extends Specification
{
  private static final List<ComponentInfo> TEST_COMPONENT_LIST_RETURN = [new ComponentInfo("foo", "bar", "1.0")]

  private static final List<SearchParameter> DEFAULT_SEARCH_PARAMS = [new SearchParameter('q', 'foo')]

  private static final Map<String, String> DEFAULT_SEARCH = ['q': 'foo']

  private static final String DEFAULT_TAG = 'foo-tag'

  private static final String DEFAULT_INSTANCE = 'localhost'

  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule()

  RepositoryManagerV3Client nxrm3Client = Mock(RepositoryManagerV3Client)

  def 'displays correct nexus instances'() {
    setup:
      def nx3a = createNxrm3Config('nx3a')
      def nx3b = createNxrm3Config('nx3b')
      def nx2 = createNxrm2Config('nx2')
      saveNxrmConfigs([nx3a, nx3b, nx2])

    when: 'nexus instance items are filled'
      def descriptor = getDescriptor()
      def listBoxModel = descriptor.doFillNexusInstanceIdItems()

    then: 'ListBox has the correct options'
      listBoxModel.size() == 3 // two nx3 instances
      listBoxModel.findAll { it.value == 'nx3a' }.size() == 1
      listBoxModel.findAll { it.value == 'nx3b' }.size() == 1
      listBoxModel.findAll { it.value == 'nx2' }.size() == 0

    and: 'ListBox has empty item'
      listBoxModel.findAll { it.name == EMPTY_LIST_BOX_NAME && it.value == EMPTY_LIST_BOX_VALUE }.size() == 1
  }

  def 'build success when associate succeeds in freestyle'() {
    setup:
      def project = getProject(DEFAULT_INSTANCE, tag)

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      1 * nxrm3Client.associate(expectedTag, DEFAULT_SEARCH) >> TEST_COMPONENT_LIST_RETURN
      jenkinsRule.assertBuildStatus(SUCCESS, build)

    where:
      tag               | expectedTag
      DEFAULT_TAG       | DEFAULT_TAG
      'foo-${BUILD_ID}' | 'foo-1'
  }

  def 'build success when associate succeeds in workflow'() {
    setup:
      def project = getWorkflowProject()

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      1 * nxrm3Client.associate(DEFAULT_TAG, DEFAULT_SEARCH) >> TEST_COMPONENT_LIST_RETURN
      jenkinsRule.assertBuildStatus(SUCCESS, build)
  }

  def 'build failure when associate fails in freestyle'() {
    setup:
      def expectedLogMsg = 'associate failed'
      def project = getProject()

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      1 * nxrm3Client.associate(*_) >> { throw new RepositoryManagerException(expectedLogMsg) }
      jenkinsRule.assertBuildStatus(Result.FAILURE, build)
      jenkinsRule.assertLogContains(expectedLogMsg, build)
  }

  def 'build failure when associate fails in workflow'() {
    setup:
      def expectedLogMsg = 'associate failed'
      def project = getWorkflowProject()

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      1 * nxrm3Client.associate(*_) >> { throw new RepositoryManagerException(expectedLogMsg) }
      jenkinsRule.assertBuildStatus(Result.FAILURE, build)
      jenkinsRule.assertLogContains(expectedLogMsg, build)
  }

  def 'build failure when associate fails and verifies no follow-on steps are run in freestyle'() {
    setup:
      def expectedLogMessage = 'associate failed'
      def config = saveNxrm3Config(DEFAULT_INSTANCE)
      def project = jenkinsRule.createFreeStyleProject()

      def builder = new AssociateTagStep(DEFAULT_INSTANCE, DEFAULT_TAG, DEFAULT_SEARCH_PARAMS)
      project.getBuildersList().add(builder)

      // second build step which should not get executed
      def builder2 = new DeleteComponentsStep(DEFAULT_INSTANCE, DEFAULT_TAG)
      project.getBuildersList().add(builder2)

      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      RepositoryManagerClientUtil.nexus3Client(config.serverUrl, config.credentialsId) >> nxrm3Client

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      // verify the association call and no call to delete
      1 * nxrm3Client.associate(*_) >> { throw new RepositoryManagerException(expectedLogMessage) }
      0 * nxrm3Client.delete(*_)
      jenkinsRule.assertBuildStatus(Result.FAILURE, build)
      jenkinsRule.assertLogContains(expectedLogMessage, build)
  }

  def 'build failure when associate fails and verifies no follow-on steps are run in workflow'() {
    setup:
      def expectedLogMessage = 'associate failed'
      def config = saveNxrm3Config(DEFAULT_INSTANCE)
      def project = jenkinsRule.createProject(WorkflowJob.class, 'nexus-associate-tag-job')

      // setup a pipeline with two steps called
      project.setDefinition(new CpsFlowDefinition(
          "node {\n" +
              "associateTag nexusInstanceId: '${DEFAULT_INSTANCE}', tagName: '${DEFAULT_TAG}', " +
              "search: [[key: 'q', value: 'bar']]\n" +
              "deleteComponents nexusInstanceId: '${DEFAULT_INSTANCE}', tagName: '${DEFAULT_TAG}'" +
              "}"))

      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      RepositoryManagerClientUtil.nexus3Client(config.serverUrl, config.credentialsId) >> nxrm3Client

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      // verify the associate call and no call to delete
      1 * nxrm3Client.associate(*_) >> { throw new RepositoryManagerException(expectedLogMessage) }
      0 * nxrm3Client.delete(*_)
      jenkinsRule.assertBuildStatus(Result.FAILURE, build)
      jenkinsRule.assertLogContains(expectedLogMessage, build)
  }

  def 'build failure when attempting to get an nxrm3 client with invalid id'() {
    setup:
      def expectedLogMsg = 'localhost not found'
      def project = getProject(DEFAULT_INSTANCE, DEFAULT_TAG, DEFAULT_SEARCH_PARAMS,
          { throw new RepositoryManagerException(expectedLogMsg) })

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      jenkinsRule.assertBuildStatus(Result.FAILURE, build)
      jenkinsRule.assertLogContains("localhost not found", build)
  }

  @Unroll
  def 'build failure due to missing parameter in workflow - #missingParam'(stepArgs, missingParam, expectedLogMsg) {
    setup:
      def config = saveNxrm3Config('instance')

      def project = jenkinsRule.createProject(WorkflowJob.class, 'nexus-associate-tag-job')
      project.setDefinition(new CpsFlowDefinition("node { associateTag ${stepArgs} }"))

      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      RepositoryManagerClientUtil.nexus3Client(config.serverUrl, config.credentialsId) >> nxrm3Client

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      jenkinsRule.assertBuildStatus(Result.FAILURE, build)
      jenkinsRule.assertLogContains(expectedLogMsg, build)

    where:
      stepArgs << ['nexusInstanceId: "someInstance"', 'tagName: "foo"']
      missingParam << ['tagName', 'nexusInstanceId']
      expectedLogMsg << ['Tag Name is required', 'Nexus Instance ID is required']
  }

  def getDescriptor() {
    return (DescriptorImpl) jenkinsRule.getInstance().getDescriptor(AssociateTagStep.class)
  }

  def saveNxrm3Config(String id) {
    def cfg = createNxrm3Config(id)
    saveNxrmConfigs([cfg])
    cfg
  }

  def saveNxrmConfigs(List<NxrmConfiguration> nxrmConfigs) {
    def globalConfiguration = jenkinsRule.getInstance().getDescriptorByType(GlobalNexusConfiguration)
    globalConfiguration.nxrmConfigs = nxrmConfigs
    globalConfiguration.save()
  }

  def createNxrm3Config(String id) {
    new Nxrm3Configuration(id, "internal${id}", 'displayName', 'http://foo.com', 'credentialsId')
  }

  def createNxrm2Config(String id) {
    new Nxrm2Configuration(id, "internal${id}", 'displayName', 'http://foo.com', 'credentialsId')
  }

  def getProject(String instance = DEFAULT_INSTANCE,
                 String tag = DEFAULT_TAG,
                 List<SearchParameter> search = DEFAULT_SEARCH_PARAMS,
                 Closure clientReturn = { nxrm3Client })
  {
    def config = saveNxrm3Config(instance)
    def project = jenkinsRule.createFreeStyleProject()
    def builder = new AssociateTagStep(instance, tag, search)
    project.getBuildersList().add(builder)

    GroovyMock(RepositoryManagerClientUtil.class, global: true)
    RepositoryManagerClientUtil.nexus3Client(config.serverUrl, config.credentialsId) >> { clientReturn.call() }

    project
  }

  //Prepares a workflow job
  def getWorkflowProject(String instance = DEFAULT_INSTANCE,
                         String tag = DEFAULT_TAG,
                         List<SearchParameter> search = DEFAULT_SEARCH_PARAMS,
                         Closure clientReturn = { nxrm3Client })
  {
    def config = saveNxrm3Config(instance)
    def project = jenkinsRule.createProject(WorkflowJob.class, "nexus-associate-tag-job")
    def searchString = '['
    search.forEach({ searchString += "[key: '${it.key}', value: '${it.value}']," })
    searchString = searchString[0..-2] + ']'
    project.setDefinition(
        new CpsFlowDefinition(
            "node { associateTag nexusInstanceId: '${instance}', tagName: '${tag}', search: ${searchString} }"))

    GroovyMock(RepositoryManagerClientUtil.class, global: true)
    RepositoryManagerClientUtil.nexus3Client(config.serverUrl, config.credentialsId) >> { clientReturn.call() }

    project
  }
}
