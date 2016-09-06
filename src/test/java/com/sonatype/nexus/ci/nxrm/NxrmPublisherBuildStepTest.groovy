/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm

import com.sonatype.nexus.api.ApiStub.NexusClientFactory
import com.sonatype.nexus.api.ApiStub.NxrmClient

import hudson.model.Describable
import hudson.model.Result
import hudson.model.Run
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class NxrmPublisherBuildStepTest
    extends NxrmPublisherDescriptorTest
{
  @Override
  Class<? extends Describable> getDescribable() {
    return NxrmPublisherBuildStep.class
  }

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  NxrmClient nxrmClient = Mock()

  def 'it fails build when Maven asset is not available for upload'() {
    setup:
      def nxrm2Configuration = saveGlobalConfigurationWithNxrm2Configuration()
      def packageList = buildPackageList()
      def nexusPublisher = new NxrmPublisherBuildStep(nxrm2Configuration.internalId, 'maven-releases', packageList)

      def project = jenkins.createFreeStyleProject()
      project.getBuildersList().add(nexusPublisher)

      GroovyMock(NexusClientFactory.class, global: true)
      NexusClientFactory.buildRmClient(nxrm2Configuration.serverUrl, nxrm2Configuration.credentialsId) >> nxrmClient

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
      def nexusPublisher = new NxrmPublisherBuildStep(nxrm2Configuration.internalId, repositoryId, packageList)

      def project = jenkins.createFreeStyleProject()
      def workspace = temp.newFolder()
      def testJar = new File(workspace, 'test.jar')
      testJar.createNewFile()
      project.setCustomWorkspace(workspace.getAbsolutePath())
      project.getBuildersList().add(nexusPublisher)

      GroovyMock(NexusClientFactory.class, global: true)
      NexusClientFactory.buildRmClient(nxrm2Configuration.serverUrl, nxrm2Configuration.credentialsId) >> nxrmClient

      nxrmClient.uploadComponent(_, _, _) >> { throw new IOException() }

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
      def nexusPublisher = new NxrmPublisherBuildStep(nxrm2Configuration.internalId, repositoryId, packageList)

      def project = jenkins.createFreeStyleProject()
      def workspace = temp.newFolder()
      def testJar = new File(workspace, 'test.jar')
      testJar.createNewFile()
      project.setCustomWorkspace(workspace.getAbsolutePath())
      project.getBuildersList().add(nexusPublisher)

      GroovyMock(NexusClientFactory.class, global: true)
      NexusClientFactory.buildRmClient(nxrm2Configuration.serverUrl, nxrm2Configuration.credentialsId) >> nxrmClient

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
