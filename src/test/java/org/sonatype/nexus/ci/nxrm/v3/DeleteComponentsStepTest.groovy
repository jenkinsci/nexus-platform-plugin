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
import org.sonatype.nexus.ci.config.Nxrm3Configuration
import org.sonatype.nexus.ci.config.NxrmConfiguration
import org.sonatype.nexus.ci.nxrm.v3.DeleteComponentsStep.DescriptorImpl
import org.sonatype.nexus.ci.util.FormUtil
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import hudson.model.Result
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification
import spock.lang.Unroll

import static hudson.model.Result.SUCCESS

class DeleteComponentsStepTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule()

  RepositoryManagerV3Client nxrm3Client = Mock(RepositoryManagerV3Client)

  def 'it populates Nexus instances'() {
    setup:
      def nxrm3Configuration = createNxrm3Config('id')

    when: 'nexus instance items are filled'
      def descriptor = getDescriptor()
      def listBoxModel = descriptor.doFillNexusInstanceIdItems()

    then: 'ListBox has the correct size'
      listBoxModel.size() == 2

    and: 'ListBox has empty item'
      listBoxModel.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      listBoxModel.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE

    and: 'ListBox is populated'
      listBoxModel.get(1).name == nxrm3Configuration.displayName
      listBoxModel.get(1).value == nxrm3Configuration.id
  }

  def 'it successfully completes a delete operation based on a tag'() {
    setup:
      def project = getProject('localhost', tag)

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      1 * nxrm3Client.delete(expectedTag) >> Arrays.asList(new ComponentInfo("foo", "boo", "1.0"))
      jenkinsRule.assertBuildStatus(SUCCESS, build)

    where:
      tag               | expectedTag
      'foo'             | 'foo'
      'foo-${BUILD_ID}' | 'foo-1'
  }

  def 'it fails to complete a delete operation based on a tag'() {
    setup:
      def project = getProject('localhost', 'foo',
          { throw new RepositoryManagerException("Delete failed") })

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      jenkinsRule.assertBuildStatus(Result.FAILURE, build)
      jenkinsRule.assertLogContains("Delete failed", build)
  }

  def 'it fails to complete a delete operation based on a tag and verifies no follow-on steps are run'() {
    setup:
      def instance = 'localhost'
      def config = createNxrm3Config(instance)
      def project = jenkinsRule.createFreeStyleProject()

      def builder = new DeleteComponentsStep(instance, 'foo')
      project.getBuildersList().add(builder)

      //This is a second build step which should not get executed
      def builder2 = new DeleteComponentsStep(instance, 'boo')
      project.getBuildersList().add(builder2)

      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      RepositoryManagerClientUtil.nexus3Client(config.serverUrl, config.credentialsId) >> nxrm3Client

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      //Verify the delete call only happens 1 time signaling the second step never gets executed
      1 * nxrm3Client.delete(_) >> { throw new RepositoryManagerException("Delete failed") }
      jenkinsRule.assertBuildStatus(Result.FAILURE, build)
      jenkinsRule.assertLogContains("Delete failed", build)
  }

  def 'it fails to complete a delete operation based on a tag and verifies no follow-on steps are run as workflow'() {
    setup:
      def instance = 'localhost'
      def tagName = 'foo'
      def config = createNxrm3Config(instance)
      def project = jenkinsRule.createProject(WorkflowJob.class, "nexusStagingDelete")

      //We set up the workflow with 2 delete steps...we should only see the first one called
      project.setDefinition(new CpsFlowDefinition(
          "node { " +
              "\ndeleteComponents nexusInstanceId: '" + instance + "', tagName: '" + tagName + "'" +
              "\ndeleteComponents nexusInstanceId: '" + instance + "', tagName: '" + tagName + "'" +
              "}", false))

      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      RepositoryManagerClientUtil.nexus3Client(config.serverUrl, config.credentialsId) >> nxrm3Client

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      //Verify the delete call only happens 1 time signaling the second step never gets executed
      1 * nxrm3Client.delete(_) >> { throw new RepositoryManagerException("Delete failed") }
      jenkinsRule.assertBuildStatus(Result.FAILURE, build)
      jenkinsRule.assertLogContains("Delete failed", build)
  }

  def 'it fails attempting to get an nxrm3 client with invalid id'() {
    setup:
      def project = getProject('invalidclient', 'foo',
          { throw new RepositoryManagerException("localhost not found") })

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      jenkinsRule.assertBuildStatus(Result.FAILURE, build)
      jenkinsRule.assertLogContains("localhost not found", build)
  }

  def 'it successfully completes a delete operation based on a tag as workflow'() {
    setup:
      def project = getWorkflowProject('localhost', 'foo')

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      1 * nxrm3Client.delete('foo') >> Arrays.asList(new ComponentInfo("foo", "boo", "1.0"))
      jenkinsRule.assertBuildStatus(SUCCESS, build)
  }

  def 'it fails to complete a delete operation based on a tag as a workflow'() {
    setup:
      def project = getWorkflowProject('localhost', 'foo'
          , { throw new RepositoryManagerException("Delete failed") })

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      jenkinsRule.assertBuildStatus(Result.FAILURE, build)
      jenkinsRule.assertLogContains("Failing build due to: Delete failed", build)
  }

  @Unroll
  def 'it fails due to missing parameter in workflow dsl - #missingParam'(stepArgs, missingParam, expectedLogMsg) {
    setup:
      def config = createNxrm3Config('someInstance')

      def project = jenkinsRule.createProject(WorkflowJob.class, "nexusStagingDelete")
      project.setDefinition(new CpsFlowDefinition("node {deleteComponents ${stepArgs} }", false))

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
    return (DescriptorImpl) jenkinsRule.getInstance().getDescriptor(DeleteComponentsStep.class)
  }

  def createNxrm3Config(String id) {
    def configurationList = new ArrayList<NxrmConfiguration>()
    def nxrm3Configuration = new Nxrm3Configuration(id, "internal${id}",
        'displayName', 'http://foo.com', 'credentialsId')

    configurationList.push(nxrm3Configuration)

    def globalConfiguration = jenkinsRule.getInstance().getDescriptorByType(GlobalNexusConfiguration)
    globalConfiguration.nxrmConfigs = configurationList
    globalConfiguration.save()

    nxrm3Configuration
  }

  def getProject(String instance, String tag, Closure clientReturn = { nxrm3Client }) {
    def config = createNxrm3Config(instance)
    def project = jenkinsRule.createFreeStyleProject()
    def builder = new DeleteComponentsStep(instance, tag)
    project.getBuildersList().add(builder)

    GroovyMock(RepositoryManagerClientUtil.class, global: true)
    RepositoryManagerClientUtil.nexus3Client(config.serverUrl, config.credentialsId) >> { clientReturn.call() }

    project
  }

  //Prepares a workflow job
  def getWorkflowProject(String instance, String tagName, Closure clientReturn = { nxrm3Client }) {
    def config = createNxrm3Config(instance)
    def project = jenkinsRule.createProject(WorkflowJob.class, "nexusStagingDelete")
    project.setDefinition(new CpsFlowDefinition("node {deleteComponents nexusInstanceId: '" + instance +
        "', tagName: '" + tagName + "'}", false))

    GroovyMock(RepositoryManagerClientUtil.class, global: true)
    RepositoryManagerClientUtil.nexus3Client(config.serverUrl, config.credentialsId) >> { clientReturn.call() }

    project
  }
}
