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
import org.sonatype.nexus.ci.nxrm.v3.MoveComponentsStep.DescriptorImpl
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

class MoveComponentsStepTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule()

  RepositoryManagerV3Client nxrm3Client = Mock()

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

  def 'it populates the list of destination repositories'() {
    setup:
      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      def nxrm3Configuration = createNxrm3Config('id')

      def client = Mock(RepositoryManagerV3Client.class)
      def repositories = [
          [
              name: 'Maven Releases',
              format: 'maven2',
              type: 'hosted',
              repositoryPolicy: 'Release'
          ],
          [
              name: 'Maven 1 Releases',
              format: 'maven1',
              type: 'hosted',
              repositoryPolicy: 'Release'
          ],
          [
              name: 'Maven Snapshots',
              format: 'maven2',
              type: 'hosted',
              repositoryPolicy: 'Snapshot'
          ],
          [
              name: 'Maven Proxy',
              format: 'maven2',
              type: 'proxy',
              repositoryPolicy: 'Release'
          ]
      ]
      client.getRepositories() >> repositories
      RepositoryManagerClientUtil.nexus3Client(nxrm3Configuration.serverUrl, nxrm3Configuration.credentialsId) >> client

    when: 'destination nexus repository items are filled'
      def descriptor = getDescriptor()
      def listBoxModel = descriptor.doFillDestinationItems(nxrm3Configuration.id)

    then: 'ListBox has the correct size'
      //only looking for 3 because nxrm3 client only looks for hosted maven repos when populating this list
      listBoxModel.size() == 3

    and: 'ListBox has empty item'
      listBoxModel.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      listBoxModel.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE

    and: 'ListBox is populated'
      listBoxModel.get(1).name == repositories.get(0).name
      listBoxModel.get(1).value == repositories.get(0).name
  }

  def 'it successfully completes a move operation based on a tag'() {
    setup:
      def project = getProject('localhost', 'maven-releases', 'foo')

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      1 * nxrm3Client.move('maven-releases', 'foo') >> Arrays.asList(new ComponentInfo("foo", "boo", "1.0"))
      jenkinsRule.assertBuildStatus(SUCCESS, build)
  }

  def 'it fails to complete a move operation based on a tag'() {
    setup:
      def project = getProject('localhost', 'maven-releases', 'foo',
          { throw new RepositoryManagerException("Move failed") })

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      jenkinsRule.assertBuildStatus(Result.FAILURE, build)
      jenkinsRule.assertLogContains("Move failed", build)
  }

  def 'it fails attempting to get an nxrm3 client with invalid id'() {
    setup:
      def project = getProject('invalidclient', 'maven-releases', 'foo',
          { throw new RepositoryManagerException("localhost not found") } )

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      jenkinsRule.assertBuildStatus(Result.FAILURE, build)
      jenkinsRule.assertLogContains("localhost not found", build)
  }

  def 'it successfully completes a move operation based on a tag as workflow'() {
    setup:
      def project = getWorkflowProject('localhost', 'maven-releases', 'foo')

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      1 * nxrm3Client.move('maven-releases', 'foo') >> Arrays.asList(new ComponentInfo("foo", "boo", "1.0"))
      jenkinsRule.assertBuildStatus(SUCCESS, build)
  }

  def 'it fails to complete a move operation based on a tag as a workflow'() {
      setup:
        def project = getWorkflowProject('localhost', 'maven-releases', 'foo'
            , { throw new RepositoryManagerException("Move failed") })

      when:
        def build = project.scheduleBuild2(0).get()

      then:
        jenkinsRule.assertBuildStatus(Result.FAILURE, build)
        jenkinsRule.assertLogContains("Failing build due to: Move failed", build)
    }

  @Unroll
  def 'it fails due to missing parameter in workflow dsl - #missingParam'(stepArgs, missingParam, expectedLogMsg) {
    setup:
      def config = createNxrm3Config('someInstance')

      def project = jenkinsRule.createProject(WorkflowJob.class, "nexusStagingMove")
      project.setDefinition(new CpsFlowDefinition("node {moveComponents ${stepArgs} }"))

      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      RepositoryManagerClientUtil.nexus3Client(config.serverUrl, config.credentialsId) >> nxrm3Client

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      jenkinsRule.assertBuildStatus(Result.FAILURE, build)
      jenkinsRule.assertLogContains(expectedLogMsg, build)

    where:
      stepArgs << ['tagName: "foo", nexusInstanceId: "someInstance"',
                   'destination: "maven-releases", nexusInstanceId: "someInstance"',
                   'destination: "maven-releases", tagName: "foo"']
      missingParam << ['destination', 'tagName', 'nexusInstanceId']
      expectedLogMsg << ['Destination is required', 'Tag Name is required', 'Nexus Instance ID is required']
  }

  def getDescriptor() {
    return (DescriptorImpl) jenkinsRule.getInstance().getDescriptor(MoveComponentsStep.class)
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

  def getProject(String instance, String destination, String tag, Closure clientReturn = { nxrm3Client }) {
    def config = createNxrm3Config(instance)
    def project = jenkinsRule.createFreeStyleProject()
    def builder = new MoveComponentsStep(instance, tag, destination)
    project.getBuildersList().add(builder)

    GroovyMock(RepositoryManagerClientUtil.class, global: true)
    RepositoryManagerClientUtil.nexus3Client(config.serverUrl, config.credentialsId) >> { clientReturn.call() }

    project
  }

  //Prepares a workflow job
  def getWorkflowProject(String instance, String destination, String tagName, Closure clientReturn = { nxrm3Client }) {
    def config = createNxrm3Config(instance)
    def project = jenkinsRule.createProject(WorkflowJob.class, "nexusStagingMove")
    project.setDefinition(new CpsFlowDefinition("node {moveComponents destination: '" + destination +
        "', nexusInstanceId: '" + instance +"', tagName: '" + tagName + "'}"))

    GroovyMock(RepositoryManagerClientUtil.class, global: true)
    RepositoryManagerClientUtil.nexus3Client(config.serverUrl, config.credentialsId) >> { clientReturn.call() }

    project
  }
}
