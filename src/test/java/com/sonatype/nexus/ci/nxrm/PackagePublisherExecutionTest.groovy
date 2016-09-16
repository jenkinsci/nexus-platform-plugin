/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.nxrm

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class PackagePublisherExecutionTest
    extends NexusPublisherDescriptorTest
{
  @Override
  NexusPublisherDescriptor getDescriptor() {
    return (NexusPublisherDescriptor) jenkins.getInstance().getDescriptor(NexusPublisherWorkflowStep.class)
  }

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  def 'it calls uploadPackage'() {
    setup:
      def repositoryId = 'maven-releases'
      def nexus2Configuration = saveGlobalConfigurationWithNxrm2Configuration()
      def packageList = buildPackageList()
      def nexusPublisher = new NexusPublisherWorkflowStep(nexus2Configuration.id, repositoryId, packageList)

      GroovyMock(PackageUploaderUtil.class, global: true)
      TaskListener taskListener = Mock()
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      PrintStream logger = new PrintStream(os);

      taskListener.getLogger() >> logger
      Run run = null
      FilePath filePath = new FilePath(temp.newFile())
      NexusPublisherWorkflowStep nxrmPublisher = nexusPublisher

      PackagePublisherExecution underTest = new PackagePublisherExecution()
      underTest.nxrmPublisher = nxrmPublisher
      underTest.taskListener = taskListener
      underTest.run = run
      underTest.filePath = filePath

    when:
      underTest.run()

    then:
      1 * PackageUploaderUtil.uploadPackage(_, _, _, _)

    and:
      logger.out.toString() =~ /Upload of .* succeeded./
  }

  def 'it bubbles up IOException when things go wrong'() {
    setup:
      def repositoryId = 'maven-releases'
      def nxrm2Configuration = saveGlobalConfigurationWithNxrm2Configuration()
      def packageList = buildPackageList()
      def nexusPublisher = new NexusPublisherWorkflowStep(nxrm2Configuration.id, repositoryId, packageList)

      GroovyMock(PackageUploaderUtil.class, global: true)
      TaskListener taskListener = Mock()
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      PrintStream logger = new PrintStream(os);

      taskListener.getLogger() >> logger
      Run run = null
      FilePath filePath = new FilePath(temp.newFile())
      NexusPublisherWorkflowStep nxrmPublisher = nexusPublisher

      PackagePublisherExecution underTest = new PackagePublisherExecution()
      underTest.nxrmPublisher = nxrmPublisher
      underTest.taskListener = taskListener
      underTest.run = run
      underTest.filePath = filePath

    PackageUploaderUtil.uploadPackage(_, _, _, _) >> { throw new IOException("oops") }

    when:
      underTest.run()

    then:
      IOException ex = thrown()
      ex.message =~ /oops/

    and:
      logger.out.toString() =~ /Upload of .* failed./
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
