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

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

import com.sonatype.nexus.api.exception.RepositoryManagerException
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client
import com.sonatype.nexus.api.repository.v3.Tag

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.Nxrm2Configuration
import org.sonatype.nexus.ci.config.Nxrm3Configuration
import org.sonatype.nexus.ci.nxrm.Messages
import org.sonatype.nexus.ci.nxrm.v3.CreateTagStep.DescriptorImpl
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import hudson.model.Result
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification
import spock.lang.Unroll

import static hudson.util.FormValidation.Kind.ERROR
import static hudson.util.FormValidation.Kind.OK
import static org.sonatype.nexus.ci.nxrm.Messages.Common_Validation_Staging_TagNameRequired
import static org.sonatype.nexus.ci.nxrm.Messages.CreateTag_Validation_TagAttributesJson

class CreateTagStepTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  @Rule
  public TemporaryFolder temp = new TemporaryFolder()

  RepositoryManagerV3Client nxrm3Client = Mock()

  def 'creates a tag using workspace'() {
    setup:
      def job = prepareJob('nx3', tag)
      def attrFile = Paths.get(job.workspace.absolutePath, 'attr-file.json')
      Files.write(attrFile, '{"foo": "bar"}'.bytes, StandardOpenOption.CREATE_NEW) // create file in workspace

      job.builder.setTagAttributesPath('attr-file.json') // uses relative path in workspace
      job.builder.setTagAttributesJson('{"baz": "qux"}')

    when:
      def build = job.project.scheduleBuild2(0).get()

    then:
      1 * nxrm3Client.createTag(expectedTag, [foo: 'bar', baz: 'qux']) >> new Tag(expectedTag, [foo: 'bar', baz: 'qux'])
      jenkins.assertBuildStatus(Result.SUCCESS, build)

    where:
      tag               | expectedTag
      'foo'             | 'foo'
      'foo-${BUILD_ID}' | 'foo-1'
  }

  def 'runnable from pipeline'() {
    setup:
      def instance = 'pipeline'
      def tagName = 'pipeline-test-tag'
      def attrFileName = 'attr-file.json'
      def config = saveNxrmConfig(instance)
      def project = jenkins.createProject(WorkflowJob, instance)
      Files.createDirectories(Paths.get(jenkins.jenkins.getWorkspaceFor(project).toURI()))
      project.setDefinition(new CpsFlowDefinition("""
          node {
            def file = new File("\${WORKSPACE}/${attrFileName}")
            file.createNewFile()
            file << '{"foo": "bar"}'
            createTag nexusInstanceId: '${instance}', tagName: '${tagName}', tagAttributesPath: '${attrFileName}'
          }
          """, false))

      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      RepositoryManagerClientUtil.nexus3Client(instance) >> nxrm3Client
      RepositoryManagerClientUtil.nexus3Client(config.serverUrl, config.credentialsId) >> nxrm3Client

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      1 * nxrm3Client.createTag(tagName, [foo: 'bar']) >> new Tag(tagName, [foo: 'bar'])
      jenkins.assertBuildStatus(Result.SUCCESS, build)
  }

  @Unroll
  def 'tag attributes are optional - #description'(tagName, attributesFile, attributesJson, attributeMap,
                                                   description)
  {
    setup:
      def job = prepareJob('nx3', tagName)

      if (attributesFile) {
        def attrFile = Paths.get(job.workspace.absolutePath, 'attr-file.json')
        Files.write(attrFile, attributesFile.bytes, StandardOpenOption.CREATE_NEW)
        job.builder.setTagAttributesPath('attr-file.json')
      }

      if (attributesJson) {
        job.builder.setTagAttributesJson(attributesJson)
      }

    when:
      def build = job.project.scheduleBuild2(0).get()

    then:
      1 * nxrm3Client.createTag(tagName, attributeMap) >> new Tag(tagName, attributeMap ?: [:])
      jenkins.assertBuildStatus(Result.SUCCESS, build)

    where:
      tagName << ['tagFoo', 'tagBar', 'tagBaz']
      attributesFile << [null, '{"baz": "qux"}', null]
      attributesJson << ['{"foo": "bar"}', null, null]
      attributeMap << [[foo: 'bar'], [baz: 'qux'], null]
      description << ['no attribute file', 'no attribute json', 'no attributes']
  }

  def 'tag attribute json has priority over file attributes'() {
    setup:
      def tagName = 'create-tag-test'
      def job = prepareJob('nx3', tagName)
      def attrFile = Paths.get(job.workspace.absolutePath, 'attr-file.json')
      Files.write(attrFile, '{"foo": "bar", "baz": "qux"}'.bytes,
          StandardOpenOption.CREATE_NEW) // create file in workspace

      job.builder.setTagAttributesPath('attr-file.json') // uses relative path in workspace
      job.builder.setTagAttributesJson('{"foo": "quux"}')

    when:
      def build = job.project.scheduleBuild2(0).get()

    then:
      1 * nxrm3Client.createTag(tagName, [foo: 'quux', baz: 'qux']) >> new Tag(tagName, [foo: 'quux', baz: 'qux'])
      jenkins.assertBuildStatus(Result.SUCCESS, build)
  }

  @Unroll
  def 'descriptor validates tag name - #description'(tagName, validationKind, validationMessage, description) {
    setup:
      saveNxrmConfig('validateTag')
      def descriptor = (DescriptorImpl) jenkins.getInstance().getDescriptor(CreateTagStep)

    when: 'tag name is set'
      def validation = descriptor.doCheckTagName(tagName)

    then: 'it validates the tag name'
      validation.kind == validationKind
      validation.message == validationMessage

    where:
      tagName << ['valid-tag', '']
      validationKind << [OK, ERROR]
      validationMessage << [null, Common_Validation_Staging_TagNameRequired()]
      description << ['valid tag', 'cant be empty']
  }

  @Unroll
  def 'descriptor validates json'(attributesJson, validationKind, validationMessage) {
    setup:
      saveNxrmConfig('validateJson')
      def descriptor = (DescriptorImpl) jenkins.getInstance().getDescriptor(CreateTagStep)

    when: 'attributes json is specified'
      def validation = descriptor.doCheckTagAttributesJson(attributesJson)

    then: 'it validates the json string'
      validation.kind == validationKind
      validation.message == validationMessage

    where:
      attributesJson << ['{ "good": "json" }', 'invalid {} json string']
      validationKind << [OK, ERROR]
      validationMessage << [null, CreateTag_Validation_TagAttributesJson()]
  }

  def 'config ui does not show nxrm2 instances'() {
    setup:
      def configs = []
      configs << new Nxrm3Configuration('nx3', "internalNx3", 'displayName', 'http://foo.com',
          'credentialsId')
      configs << new Nxrm2Configuration('nx2', "internalNx2", 'displayName', 'http://foo.com',
          'credentialsId')

      def globalConfiguration = jenkins.getInstance().getDescriptorByType(GlobalNexusConfiguration)
      globalConfiguration.nxrmConfigs = configs
      globalConfiguration.save()

      def descriptor = (DescriptorImpl) jenkins.getInstance().getDescriptor(CreateTagStep)
    when:
      def items = descriptor.doFillNexusInstanceIdItems()

    then:
      items.findAll { it.value == 'nx2' }.size() == 0
  }

  def 'fails build if client cannot be built'() {
    setup:
      def job = prepareJob('failClient', 'foo', { throw new RepositoryManagerException("bad client") })

    when:
      def build = job.project.scheduleBuild2(0).get()

    then:
      String log = jenkins.getLog(build)
      jenkins.assertBuildStatus(Result.FAILURE, build)
      log =~ 'bad client'
  }

  def 'fails build on attribute file error'() {
    setup:
      def job = prepareJob('fileError', 'foo')
      job.builder.setTagAttributesPath('attr-file-does-not-exist.json')

    when:
      def build = job.project.scheduleBuild2(0).get()

    then:
      String log = jenkins.getLog(build)
      jenkins.assertBuildStatus(Result.FAILURE, build)
      log =~ Messages.CreateTag_Error_TagAttributesPath()
  }

  def 'fails build on attribute json error'() {
    setup:
      def job = prepareJob('jsonError', 'foo')
      job.builder.setTagAttributesJson('bad/{}json')

    when:
      def build = job.project.scheduleBuild2(0).get()

    then:
      String log = jenkins.getLog(build)
      jenkins.assertBuildStatus(Result.FAILURE, build)
      log =~ Messages.CreateTag_Error_TagAttributesJson()
  }

  def 'fails build on nxrm create tag error'() {
    setup:
      def job = prepareJob('nxrmError', 'foo')

      nxrm3Client.createTag('foo', _) >> { throw new RepositoryManagerException('some create failure') }
    when:
      def build = job.project.scheduleBuild2(0).get()

    then:
      String log = jenkins.getLog(build)
      jenkins.assertBuildStatus(Result.FAILURE, build)
      log =~ 'some create failure'
  }

  def 'fails build if an nxrm2 server is used'() {
    setup:
      def configs = []
      configs << new Nxrm2Configuration('nx2', "internalNx2", 'displayName', 'http://foo.com',
          'credentialsId')
      def globalConfiguration = jenkins.getInstance().getDescriptorByType(GlobalNexusConfiguration)
      globalConfiguration.nxrmConfigs = configs
      globalConfiguration.save()

      def project = jenkins.createFreeStyleProject()
      def workspace = temp.newFolder()
      def builder = new CreateTagStep('nx2', 'foo')

      project.setCustomWorkspace(workspace.absolutePath)
      project.getBuildersList().add(builder)

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      String log = jenkins.getLog(build)
      jenkins.assertBuildStatus(Result.FAILURE, build)
      log =~ 'The specified instance is not a Nexus Repository Manager 3 server'
  }

  @Unroll
  def 'fails build with missing parameter - #missingParam'(stepArgs, missingParam, expectedLogMsg) {
    setup:
      def config = saveNxrmConfig('pipeline-fails')
      def project = jenkins.createProject(WorkflowJob, 'pipeline-fails')
      project.setDefinition(new CpsFlowDefinition("node { createTag ${stepArgs} } ", false))

      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      RepositoryManagerClientUtil.nexus3Client(config.serverUrl, config.credentialsId) >> nxrm3Client

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      jenkins.assertBuildStatus(Result.FAILURE, build)
      jenkins.assertLogContains(expectedLogMsg, build)

    where:
      stepArgs << ['nexusInstanceId: "pipeline-fails"', 'tagName: "foo"']
      missingParam << ['tagName', 'nexusInstanceId']
      expectedLogMsg << ['Tag Name is required', 'Nexus Instance ID is required']
  }

  Map prepareJob(String instance, String tag, Closure clientReturn = { nxrm3Client }) {
    def config = saveNxrmConfig(instance)
    def project = jenkins.createFreeStyleProject()
    def workspace = temp.newFolder()
    def builder = new CreateTagStep(instance, tag)

    project.setCustomWorkspace(workspace.absolutePath)
    project.getBuildersList().add(builder)

    GroovyMock(RepositoryManagerClientUtil.class, global: true)
    RepositoryManagerClientUtil.nexus3Client(config.serverUrl, config.credentialsId) >> { clientReturn.call() }

    [project: project, builder: builder, workspace: workspace]
  }

  Nxrm3Configuration saveNxrmConfig(String id) {
    def configs = []
    def nxrm3Configuration = new Nxrm3Configuration(id, "internal${id}", 'displayName', 'http://foo.com',
        'credentialsId')
    configs << nxrm3Configuration

    def globalConfiguration = jenkins.getInstance().getDescriptorByType(GlobalNexusConfiguration)
    globalConfiguration.nxrmConfigs = configs
    globalConfiguration.save()

    nxrm3Configuration
  }
}
