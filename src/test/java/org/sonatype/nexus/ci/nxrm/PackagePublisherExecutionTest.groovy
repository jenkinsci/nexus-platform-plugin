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

import org.sonatype.nexus.ci.nxrm.v2.ComponentUploaderNxrm2

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

      GroovyMock(ComponentUploaderFactory.class, global: true)
      def componentUploader = Mock(ComponentUploaderNxrm2)
      ComponentUploaderFactory.getComponentUploader(nexus2Configuration.id, run, taskListener) >> componentUploader

    when:
      underTest.run()

    then:
      1 * componentUploader.uploadComponents(nxrmPublisher, filePath, null)
  }

  def 'it bubbles up IOException when things go wrong'() {
    setup:
      def repositoryId = 'maven-releases'
      def nxrm2Configuration = saveGlobalConfigurationWithNxrm2Configuration()
      def packageList = buildPackageList()
      def nexusPublisher = new NexusPublisherWorkflowStep(nxrm2Configuration.id, repositoryId, packageList)

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

      GroovyMock(ComponentUploaderFactory.class, global: true)
      def componentUploader = Mock(ComponentUploaderNxrm2)
      ComponentUploaderFactory.getComponentUploader(nxrm2Configuration.id,  run, taskListener) >> componentUploader

      componentUploader.uploadComponents(nexusPublisher, filePath, null) >> { throw new IOException("oops") }

    when:
      underTest.run()

    then:
      IOException ex = thrown(IOException)
      ex.message =~ /oops/
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
