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

import com.sonatype.nexus.api.exception.RepositoryManagerException
import com.sonatype.nexus.api.repository.v2.RepositoryManagerClient

import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import hudson.model.Result
import hudson.model.Run
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class NexusPublisherBuildStepTest
    extends NexusPublisherDescriptorTest
{
  @Override
  NexusPublisherDescriptor getDescriptor() {
    return (NexusPublisherDescriptor) jenkins.getInstance().getDescriptor(NexusPublisherBuildStep.class)
  }

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  RepositoryManagerClient nxrmClient = Mock()

  def 'it fails build when Maven asset is not available for upload'() {
    setup:
      def nxrm2Configuration = saveGlobalConfigurationWithNxrm2Configuration()
      def packageList = buildPackageList()
      def nexusPublisher = new NexusPublisherBuildStep(nxrm2Configuration.id, 'maven-releases', packageList)

      def project = jenkins.createFreeStyleProject()
      project.getBuildersList().add(nexusPublisher)

      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      RepositoryManagerClientUtil.newRepositoryManagerClient(nxrm2Configuration.serverUrl, nxrm2Configuration.credentialsId) >> nxrmClient
      RepositoryManagerClientUtil.nexus2Client(nxrm2Configuration.serverUrl, nxrm2Configuration.credentialsId) >> nxrmClient

    when:
      Run build = project.scheduleBuild2(0).get()

    then:
      jenkins.assertBuildStatus(Result.FAILURE, build)

    and:
      String log = jenkins.getLog(build)
      log =~ /test.jar does not exist/
      log =~ /Failing build due to missing expected files for Nexus Repository Manager Publisher/
  }

  def 'it fails build when uploads to Nexus Repository Manager fails'() {
    setup:
      def repositoryId = 'maven-releases'
      def nxrm2Configuration = saveGlobalConfigurationWithNxrm2Configuration()
      def packageList = buildPackageList()
      def nexusPublisher = new NexusPublisherBuildStep(nxrm2Configuration.id, repositoryId, packageList)

      def project = jenkins.createFreeStyleProject()
      def workspace = temp.newFolder()
      def testJar = new File(workspace, 'test.jar')
      testJar.createNewFile()
      project.setCustomWorkspace(workspace.getAbsolutePath())
      project.getBuildersList().add(nexusPublisher)

      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      RepositoryManagerClientUtil.newRepositoryManagerClient(nxrm2Configuration.serverUrl, nxrm2Configuration.credentialsId) >> nxrmClient
      RepositoryManagerClientUtil.nexus2Client(nxrm2Configuration.serverUrl, nxrm2Configuration.credentialsId) >> nxrmClient

      nxrmClient.uploadComponent(_, _, _) >> { throw new RepositoryManagerException("something went wrong") }

    when:
      Run build = project.scheduleBuild2(0).get()

    then:
      jenkins.assertBuildStatus(Result.FAILURE, build)

    and:
      String log = jenkins.getLog(build)
      log =~ /Upload of test.jar failed/
      log =~ /Failing build due to failure to upload file to Nexus Repository Manager Publisher/
  }

  def 'it uploads a Maven package to Nexus Repository Manager'() {
    setup:
      def repositoryId = 'maven-releases'
      def nxrm2Configuration = saveGlobalConfigurationWithNxrm2Configuration()
      def packageList = buildPackageList()
      def nexusPublisher = new NexusPublisherBuildStep(nxrm2Configuration.id, repositoryId, packageList)

      def project = jenkins.createFreeStyleProject()
      def workspace = temp.newFolder()
      def testJar = new File(workspace, 'test.jar')
      testJar.createNewFile()
      project.setCustomWorkspace(workspace.getAbsolutePath())
      project.getBuildersList().add(nexusPublisher)

      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      RepositoryManagerClientUtil.newRepositoryManagerClient(nxrm2Configuration.serverUrl, nxrm2Configuration.credentialsId) >> nxrmClient
      RepositoryManagerClientUtil.nexus2Client(nxrm2Configuration.serverUrl, nxrm2Configuration.credentialsId) >> nxrmClient

    when:
      Run build = project.scheduleBuild2(0).get()

    then:
      jenkins.assertBuildStatus(Result.SUCCESS, build)
  }

  private List<Package> buildPackageList() {
    def mavenCoordinate = new MavenCoordinate('groupId', 'artifactId', 'version', 'pom')
    def mavenAsset = new MavenAsset('test.jar', 'classifier', 'extension')
    def mavenPackage = new MavenPackage(mavenCoordinate, [mavenAsset])
    def packageList = new ArrayList<Package>()
    packageList.add(mavenPackage)

    return packageList
  }
}
