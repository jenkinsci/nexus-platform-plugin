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

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.Nxrm2Configuration
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import hudson.EnvVars
import hudson.FilePath
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.WithoutJenkins
import spock.lang.Specification
import spock.lang.Unroll

class ComponentUploaderTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  def run = Mock(Run)

  def taskListener = Mock(TaskListener)

  ComponentUploader componentUploader

  def setup() {
    componentUploader = new ComponentUploader(run, taskListener)
  }

  @WithoutJenkins
  def 'it builds a repository client from configuration'() {
    setup:
      def nexusConfiguration = new Nxrm2Configuration('id', 'internalId', 'displayName', 'serverUrl', 'credentialsId')
      def expectedClient = Mock(RepositoryManagerV2Client)

      GroovyMock(RepositoryManagerClientUtil, global: true)
      RepositoryManagerClientUtil.newRepositoryManagerClient('serverUrl', 'credentialsId') >> expectedClient

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
      RepositoryManagerClientUtil.newRepositoryManagerClient('serverUrl', 'credentialsId') >> {
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

  def 'it gets the Nexus configuration'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxrmConfiguration = new Nxrm2Configuration('id', 'internalId', 'displayName', 'http://localhost', 'credId')
      globalConfiguration.nxrmConfigs = []
      globalConfiguration.nxrmConfigs.add(nxrmConfiguration)
      globalConfiguration.save()

    when:
      def configuration = componentUploader.getNexusConfiguration('id')

    then:
      configuration == nxrmConfiguration
  }

  def 'it fails the build if Nexus configuration not available'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxrmConfiguration = new Nxrm2Configuration('id', 'internalId', 'displayName', 'http://localhost', 'credId')
      globalConfiguration.nxrmConfigs = []
      globalConfiguration.nxrmConfigs.add(nxrmConfiguration)
      globalConfiguration.save()
      def errorMessage = ''

    when:
      try {
        componentUploader.getNexusConfiguration('other')
      }
      catch (Exception ex) {
        errorMessage = ex.message
      }

    then:
      1 * run.setResult(Result.FAILURE)
      errorMessage == 'Nexus Configuration other not found.'
  }

  def 'it fails the build if Nexus server uri is not valid'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxrmConfiguration = new Nxrm2Configuration('id', 'internalId', 'displayName', 'foo', 'credId')
      globalConfiguration.nxrmConfigs = []
      globalConfiguration.nxrmConfigs.add(nxrmConfiguration)
      globalConfiguration.save()
      def errorMessage = ''

    when:
      try {
        componentUploader.getNexusConfiguration('id')
      }
      catch (Exception ex) {
        errorMessage = ex.message
      }

    then:
      1 * run.setResult(Result.FAILURE)
      errorMessage == 'Nexus Server URL foo is invalid.'
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

      def mockComponentUploader = Spy(ComponentUploader, constructorArgs: [run, taskListener]) {
        getNexusConfiguration(_) >> nxrmConfiguration
        getRepositoryManagerClient(nxrmConfiguration) >> client
      }
      def publisher = Mock(NexusPublisher)
      def tempFile = File.createTempFile("temp", ".tmp");
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
      asset << [ new MavenAsset(null, 'classifier', 'extension'), new MavenAsset(null, '$CLASSIFIER', '$EXTENSION') ]
      expectedCoordinate <<
          [new MavenCoordinate('some-group', 'some-artifact', '1.0.0-SNAPSHOT', 'jar'),
           new MavenCoordinate('some-env-group', 'some-env-artifact', '1.0.0-01', 'jar')]
      expectedAsset <<
          [new MavenAsset(null, 'classifier', 'extension'), new MavenAsset(null, 'env-classifier', 'env-extension')]
  }
}
