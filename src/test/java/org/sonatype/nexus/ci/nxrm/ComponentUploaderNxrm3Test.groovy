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
package org.sonatype.nexus.ci.nxrm

import com.sonatype.nexus.api.repository.v3.Component
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client

import org.sonatype.nexus.ci.config.Nxrm2Configuration
import org.sonatype.nexus.ci.config.Nxrm3Configuration
import org.sonatype.nexus.ci.nxrm.ComponentUploader.RemoteMavenAsset
import org.sonatype.nexus.ci.nxrm.v3.ComponentUploaderNxrm3
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import hudson.EnvVars
import hudson.FilePath
import hudson.model.Result
import hudson.model.TaskListener
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.WithoutJenkins
import spock.lang.Unroll

class ComponentUploaderNxrm3Test
    extends ComponentUploaderTest
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  ComponentUploaderNxrm3 componentUploader

  @Override
  ComponentUploader getUploader() {
    return componentUploader
  }

  def setup() {
    def nexusConfiguration = new Nxrm3Configuration('id', 'internalId', 'displayName', 'serverUrl', 'credentialsId')
    componentUploader = new ComponentUploaderNxrm3(nexusConfiguration, run, taskListener)
  }

  @WithoutJenkins
  def 'it builds a repository client from configuration'() {
    setup:
      def nexusConfiguration = new Nxrm3Configuration('id', 'internalId', 'displayName', 'serverUrl', 'credentialsId')
      def expectedClient = Mock(RepositoryManagerV3Client)

      GroovyMock(RepositoryManagerClientUtil, global: true)
      RepositoryManagerClientUtil.nexus3Client('serverUrl', 'credentialsId') >> expectedClient

    when:
      def actualClient = componentUploader.getRepositoryManagerClient(nexusConfiguration)

    then:
      expectedClient == actualClient
  }

  @WithoutJenkins
  def 'it fails the build when a client cannot be created'() {
    setup:
      def nexusConfiguration = new Nxrm3Configuration('id', 'internalId', 'displayName', 'serverUrl', 'credentialsId')

      GroovyMock(RepositoryManagerClientUtil, global: true)
      RepositoryManagerClientUtil.nexus3Client('serverUrl', 'credentialsId') >> {
        throw new URISyntaxException('foo', 'bar')
      }

    when:
      try {
        componentUploader.getRepositoryManagerClient(nexusConfiguration)
      }
      catch (Exception ex) {
        // no op
      }

    then:
      1 * run.setResult(Result.FAILURE)
  }

  @WithoutJenkins
  @Unroll
  def 'it uploads components - #description'(String description,
                                             EnvVars envVar,
                                             MavenCoordinate coordinate,
                                             MavenAsset asset,
                                             MavenCoordinate expectedCoordinate,
                                             MavenAsset expectedAsset)
  {
    setup:
      def client = Mock(RepositoryManagerV3Client)
      def nxrmConfiguration = new Nxrm3Configuration('id', 'internalId', 'displayName', 'foo', 'credId')
      run.getEnvironment(_) >> envVar

      def mockComponentUploader =
          Spy(ComponentUploaderNxrm3, constructorArgs: [nxrmConfiguration, run, taskListener]) {
            getRepositoryManagerClient(nxrmConfiguration) >> client
          }
      def publisher = Mock(NexusPublisher)
      def tempFile = File.createTempFile("temp", ".tmp")
      tempFile.deleteOnExit()

      def filePath = new FilePath(tempFile.getParentFile())
      publisher.packages >> [
          new MavenPackage(coordinate, [new MavenAsset(tempFile.name, asset.classifier, asset.extension)])
      ]
      def repo = null
      def component = null

    when:
      mockComponentUploader.uploadComponents(publisher, filePath)
      tempFile.delete()

    then:
      1 * client.upload(*_) >> { args ->
        repo = args[0]
        component = args[1]
      }

      component.attributes.groupId == expectedCoordinate.groupId
      component.attributes.artifactId == expectedCoordinate.artifactId
      component.attributes.version == expectedCoordinate.version
      component.attributes.packaging == expectedCoordinate.packaging

      component.assets[0].getAttribute('classifier') == expectedAsset.classifier
      component.assets[0].getAttribute('extension') == expectedAsset.extension

    where:
      description << ['default', 'envVars']
      envVar << [new EnvVars([:]), new EnvVars(['GROUPID'   : 'some-env-group',
                                                'ARTIFACTID': 'some-env-artifact',
                                                'VERSION'   : '1.0.0-01',
                                                'PACKAGING' : 'jar',
                                                'CLASSIFIER': 'env-classifier',
                                                'EXTENSION' : 'env-extension'])]
      coordinate <<
          [new MavenCoordinate('some-group', 'some-artifact', '1.0.0-SNAPSHOT', 'jar'),
           new MavenCoordinate('$GROUPID', '$ARTIFACTID', '$VERSION', '$PACKAGING')]
      asset << [new MavenAsset(null, 'classifier', 'extension'), new MavenAsset(null, '$CLASSIFIER', '$EXTENSION')]
      expectedCoordinate <<
          [new MavenCoordinate('some-group', 'some-artifact', '1.0.0-SNAPSHOT', 'jar'),
           new MavenCoordinate('some-env-group', 'some-env-artifact', '1.0.0-01', 'jar')]
      expectedAsset <<
          [new MavenAsset(null, 'classifier', 'extension'), new MavenAsset(null, 'env-classifier', 'env-extension')]
  }

  @WithoutJenkins
  @Unroll
  def 'upload component groupId: #groupId, artifactId: #artifactId, version: #version, tag: #tagName'() {
    setup:
      def client = Mock(RepositoryManagerV3Client)
      def nxrmConfiguration = new Nxrm3Configuration('id', 'internalId', 'displayName', 'foo', 'credId')
      run.getEnvironment(_ as TaskListener) >> new EnvVars([:])
      client.getTag(_ as String) >> Optional.empty()

      ComponentUploaderNxrm3 mockComponentUploader =
          Spy([constructorArgs: [nxrmConfiguration, run, taskListener]] as Map<String, Object>,
              ComponentUploaderNxrm3) {
            it.getRepositoryManagerClient(nxrmConfiguration) >> client
          }
      def publisher = Mock(NexusPublisher)
      def tempFile = File.createTempFile("temp", ".tmp")
      tempFile.deleteOnExit()

      def coordinate = new MavenCoordinate(groupId, artifactId, version, packaging)
      def filePath = new FilePath(tempFile.getParentFile())
      publisher.packages >> [
          new MavenPackage(coordinate, [new MavenAsset(tempFile.name, classifier, extension)])
      ]
      String repo = null
      Component component = null
      String tag = null
      def expectedCreateTag = expectedTagName ? 1 : 0

    when:
      mockComponentUploader.uploadComponents(publisher, filePath, tagName)
      tempFile.delete()

    then:
      expectedCreateTag * client.createTag(tagName)
      1 * client.upload(*_) >> { args ->
        repo = args[0]
        component = args[1]
        tag = args[2]
      }

      component.attributes.groupId == groupId
      component.attributes.artifactId == artifactId
      component.attributes.version == version
      component.attributes.packaging == packaging

      component.assets[0].getAttribute('classifier') == classifier
      component.assets[0].getAttribute('extension') == extension

      tag == expectedTagName

    where:
      groupId      | artifactId      | version | packaging | classifier | extension | tagName  | expectedTagName
      'some-group' | 'some-artifact' | '1.0'   | 'jar'     | null       | 'jar'     | 'foobar' | 'foobar'
      'some-group' | 'some-artifact' | '2.0'   | 'jar'     | null       | 'jar'     | ''       | null
      'some-group' | 'some-artifact' | '3.0'   | 'jar'     | null       | 'jar'     | null     | null
  }

  @WithoutJenkins
  def 'it uploads the remote path from stream'() {
    setup:
      def client = Mock(RepositoryManagerV3Client)
      def nxrmConfiguration = new Nxrm3Configuration('id', 'internalId', 'displayName', 'foo', 'credId')
      run.getEnvironment(_) >> new EnvVars([:])
      def workspace = GroovyMock(FilePath)
      def remotePath = GroovyMock(FilePath)
      def coordinate = new MavenCoordinate('group', 'artifact', 'version', 'packaging')
      def remotes = new HashMap<MavenCoordinate, List<RemoteMavenAsset>>()
      remotes.
          put(coordinate,
              [new RemoteMavenAsset(new MavenAsset('bar', 'classifier', 'extension'), remotePath)])
      def mockComponentUploader =
          Spy(ComponentUploaderNxrm3, constructorArgs: [nxrmConfiguration, run, taskListener]) {
            getRepositoryManagerClient(nxrmConfiguration) >> client
          }
      mockComponentUploader.uploadComponents(*_) >> { mockComponentUploader.upload(remotes, 'repo') }
      def publisher = Mock(NexusPublisher)
      def payload = Mock(InputStream)

    when:
      mockComponentUploader.uploadComponents(publisher, workspace)

    then:
      1 * remotePath.getRemote() >> 'foo'
      1 * remotePath.read() >> payload
  }

  @WithoutJenkins
  def 'it requires a nxrm3 server'() {
    setup:
      def config = new Nxrm2Configuration('id', 'interalId', 'displayName', 'http://localhost', 'credsId')
      def logger = Mock(PrintStream)
      def publisher = Mock(NexusPublisher)
      def workspace = GroovyMock(FilePath)
      taskListener.getLogger() >> logger
      run.getEnvironment() >> new EnvVars([:])
    when:
      new ComponentUploaderNxrm3(config, run, taskListener).uploadComponents(publisher, workspace)
    then:
      def thrown = thrown(IllegalArgumentException)
      thrown.message == 'Nexus Repository Manager 3.x server is required'
      1 * logger.println('Failing build due to error creating RepositoryManagerClient')
      1 * run.setResult(Result.FAILURE)
  }
}
