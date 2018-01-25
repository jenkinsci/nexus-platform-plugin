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
package org.sonatype.nexus.ci.nxrm.v2

import com.sonatype.nexus.api.repository.v2.RepositoryManagerClient

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.NexusVersion
import org.sonatype.nexus.ci.config.NxrmConfiguration
import org.sonatype.nexus.ci.nxrm.ComponentUploaderFactory
import org.sonatype.nexus.ci.nxrm.MavenAsset
import org.sonatype.nexus.ci.nxrm.MavenCoordinate
import org.sonatype.nexus.ci.nxrm.MavenPackage
import org.sonatype.nexus.ci.nxrm.NexusPublisher
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import hudson.EnvVars
import hudson.FilePath
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import org.apache.commons.lang.ObjectUtils.Null
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.WithoutJenkins
import spock.lang.Specification
import spock.lang.Unroll

import static org.sonatype.nexus.ci.config.NexusVersion.NEXUS2

class ComponentUploaderImplTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  def env = Mock(EnvVars)

  def filePath = GroovyMock(FilePath)

  def logger = Mock(PrintStream)

  ComponentUploaderImpl componentUploader

  @WithoutJenkins
  def 'it builds a repository client from configuration'() {
    setup:
      def url = 'http://nexus:8081'
      def nexusConfiguration = new NxrmConfiguration('id', 'internalId', 'displayName', url, 'credentialsId',
          NEXUS2)
      def expectedClient = Mock(RepositoryManagerClient)
      componentUploader = new ComponentUploaderImpl(nexusConfiguration, filePath, env, logger)

      GroovyMock(RepositoryManagerClientUtil, global: true)
      RepositoryManagerClientUtil.newRepositoryManagerClient(url, 'credentialsId') >> expectedClient
      RepositoryManagerClientUtil.nexus2Client(url, 'credentialsId') >> expectedClient

    when:
      def actualClient = componentUploader.getRepositoryManagerClient(nexusConfiguration)

    then:
      expectedClient == actualClient
  }

  @WithoutJenkins
  def 'it fails the build when a client cannot be created'() {
    setup:
      def nexusConfiguration = new NxrmConfiguration('id', 'internalId', 'displayName', 'serverUrl', 'credentialsId',
          NEXUS2)
      componentUploader = new ComponentUploaderImpl(nexusConfiguration, filePath, env, logger)
    when:
      componentUploader.getRepositoryManagerClient(nexusConfiguration)
    then:
      thrown(IllegalArgumentException)
  }

  def 'it fails the build if Nexus configuration not available'() {
    when:
      componentUploader = new ComponentUploaderImpl(null, filePath, env, logger)
    then:
      thrown(NullPointerException)
  }

  def 'it fails the build if Nexus server uri is not valid'() {
    setup:
      def nxrmConfiguration = new NxrmConfiguration('id', 'internalId', 'displayName', 'foo', 'credId', NEXUS2)
      componentUploader = new ComponentUploaderImpl(nxrmConfiguration, filePath, env, logger)

    when:
        componentUploader.uploadComponents('foo', [])
    then:
      Exception e = thrown()
      e.message == 'Nexus Server URL foo is invalid.'
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
      def client = Mock(RepositoryManagerClient)
      def nxrmConfiguration = new NxrmConfiguration('id', 'internalId', 'displayName', 'foo', 'credId', NEXUS2)
      def publisher = Mock(NexusPublisher)
      def tempFile = File.createTempFile("temp", ".tmp")
      tempFile.deleteOnExit()
      def filePath = new FilePath(tempFile.getParentFile())
      def mockComponentUploader = Spy(ComponentUploaderImpl, constructorArgs: [nxrmConfiguration, filePath, envVar, logger]) {
        getRepositoryManagerClient(nxrmConfiguration) >> client
      }

      publisher.packages >> [
          new MavenPackage(coordinate, [new MavenAsset(tempFile.name, asset.classifier, asset.extension)])
      ]

      def gav = null
      def assets = null

    when:
      mockComponentUploader.uploadComponents('repoId', publisher.packages)
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
}
