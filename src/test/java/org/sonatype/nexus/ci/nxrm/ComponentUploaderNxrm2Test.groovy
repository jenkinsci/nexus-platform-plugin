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

import com.sonatype.nexus.api.repository.v2.RepositoryManagerV2Client

import org.sonatype.nexus.ci.config.Nxrm2Configuration
import org.sonatype.nexus.ci.config.Nxrm3Configuration
import org.sonatype.nexus.ci.nxrm.ComponentUploader.RemoteMavenAsset
import org.sonatype.nexus.ci.nxrm.v2.ComponentUploaderNxrm2
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import hudson.EnvVars
import hudson.FilePath
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.WithoutJenkins
import spock.lang.Unroll

class ComponentUploaderNxrm2Test
    extends ComponentUploaderTest
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  def run = Mock(Run)

  def taskListener = Mock(TaskListener)

  ComponentUploaderNxrm2 componentUploader

  @Override
  ComponentUploader getUploader() {
    componentUploader
  }

  def setup() {
    def nexusConfiguration = new Nxrm2Configuration('id', 'internalId', 'displayName', 'serverUrl', 'credentialsId')
    componentUploader = new ComponentUploaderNxrm2(nexusConfiguration, run, taskListener)
  }

  @WithoutJenkins
  def 'it builds a repository client from configuration'() {
    setup:
      def nexusConfiguration = new Nxrm2Configuration('id', 'internalId', 'displayName', 'serverUrl', 'credentialsId')
      def expectedClient = Mock(RepositoryManagerV2Client)

      GroovyMock(RepositoryManagerClientUtil, global: true)
      RepositoryManagerClientUtil.nexus2Client('serverUrl', 'credentialsId') >> expectedClient

    when:
      def actualClient = componentUploader.getRepositoryManagerClient(nexusConfiguration)

    then:
      expectedClient == actualClient
  }

  @WithoutJenkins
  def 'it fails the build when a client cannot be created'() {
    setup:
      def nexusConfiguration = new Nxrm2Configuration('id', 'internalId', 'displayName', 'serverUrl', 'credentialsId')

      GroovyMock(RepositoryManagerClientUtil, global: true)
      RepositoryManagerClientUtil.nexus2Client('serverUrl', 'credentialsId') >> {
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
      def client = Mock(RepositoryManagerV2Client)
      def nxrmConfiguration = new Nxrm2Configuration('id', 'internalId', 'displayName', 'foo', 'credId')
      run.getEnvironment(_) >> envVar

      def mockComponentUploader =
          Spy(ComponentUploaderNxrm2, constructorArgs: [nxrmConfiguration, run, taskListener]) {
            getRepositoryManagerClient(nxrmConfiguration) >> client
          }
      def publisher = Mock(NexusPublisher)
      def tempFile = File.createTempFile("temp", ".tmp")
      tempFile.deleteOnExit()

      def filePath = new FilePath(tempFile.getParentFile())
      publisher.packages >> [
          new MavenPackage(coordinate, [new MavenAsset(tempFile.name, asset.classifier, asset.extension)])
      ]
      def gav = null
      def assets = null

    when:
      mockComponentUploader.uploadComponents(publisher, filePath)
      tempFile.delete()

    then:
      1 * client.uploadComponent(*_) >> { args ->
        gav = args[1]
        assets = args[2]
      }

      gav.groupId == expectedCoordinate.groupId
      gav.artifactId == expectedCoordinate.artifactId
      gav.version == expectedCoordinate.version
      gav.packaging == expectedCoordinate.packaging

      assets[0].classifier == expectedAsset.classifier
      assets[0].extension == expectedAsset.extension
      !tempFile.absolutePath.equals(assets[0].file.absolutePath)

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
  def 'it copies assets locally'() {
    setup:
      def client = Mock(RepositoryManagerV2Client)
      def nxrmConfiguration = new Nxrm2Configuration('id', 'internalId', 'displayName', 'foo', 'credId')
      run.getEnvironment(_) >> new EnvVars([:])
      def workspace = GroovyMock(FilePath)
      def remotePath = GroovyMock(FilePath)
      def coordinate = new MavenCoordinate('group', 'artifact', 'version', 'packaging')
      def remotes = new HashMap<MavenCoordinate, List<RemoteMavenAsset>>()
      remotes.
          put(coordinate,
              [new RemoteMavenAsset(new MavenAsset('bar', 'classifier', 'extension'), remotePath)])
      def mockComponentUploader =
          Spy(ComponentUploaderNxrm2, constructorArgs: [nxrmConfiguration, run, taskListener]) {
            getRepositoryManagerClient(nxrmConfiguration) >> client
          }
      mockComponentUploader.uploadComponents(*_) >> { mockComponentUploader.upload(remotes, 'repo') }
      def publisher = Mock(NexusPublisher)
      def localFilePath = null

    when:
      mockComponentUploader.uploadComponents(publisher, workspace)

    then:
      1 * remotePath.getName() >> 'foo'
      1 * remotePath.copyTo(_ as FilePath) >> { args -> localFilePath = args[0] }
      def localTmpFileName = localFilePath.getName()
      localTmpFileName.startsWith('foo') && localTmpFileName.endsWith('.tmp')
  }

  @WithoutJenkins
  def 'it requires a nxrm2 server'() {
    setup:
      def config = new Nxrm3Configuration('id', 'interalId', 'displayName', 'http://localhost', 'credsId')
      def logger = Mock(PrintStream)
      def publisher = Mock(NexusPublisher)
      def workspace = GroovyMock(FilePath)
      taskListener.getLogger() >> logger
      run.getEnvironment() >> new EnvVars([:])
    when:
      new ComponentUploaderNxrm2(config, run, taskListener).uploadComponents(publisher, workspace)
    then:
      def thrown = thrown(IllegalArgumentException)
      thrown.message == 'Nexus Repository Manager 2.x server is required'
      1 * logger.println('Failing build due to error creating RepositoryManagerClient')
      1 * run.setResult(Result.FAILURE)
  }
}
