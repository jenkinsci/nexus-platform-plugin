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

import org.sonatype.nexus.ci.config.NxrmConfiguration
import org.sonatype.nexus.ci.nxrm.ComponentUploader.RemoteMavenAsset

import hudson.EnvVars
import hudson.FilePath
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import org.jvnet.hudson.test.WithoutJenkins
import spock.lang.Specification

abstract class ComponentUploaderTest
    extends Specification
{
  def run = Mock(Run)

  def taskListener = Mock(TaskListener)

  abstract ComponentUploader getUploader()

  @WithoutJenkins
  def 'it filters non maven packages'() {
    setup:
      run.getEnvironment(_) >> [:]
      def spyUploader = Spy(getUploader().class, constructorArgs: [Mock(NxrmConfiguration), run, taskListener])
      def publisher = Mock(NexusPublisher)
      def coordinate = Mock(MavenCoordinate)
      def tempFile = File.createTempFile("temp", ".tmp")
      tempFile.deleteOnExit()
      def filePath = new FilePath(tempFile.getParentFile())
      publisher.packages >> [
          new MavenPackage(coordinate, [new MavenAsset(tempFile.name, 'classifier', 'extension')]),
          new SimplePackage(Mock(Coordinate), [Mock(Asset)]),
          new SimplePackage(Mock(Coordinate), [Mock(Asset)])]

      def remotePackages = null

    when:
      spyUploader.uploadComponents(publisher, filePath)
      tempFile.delete()

    then:
      1 * spyUploader.upload(*_) >> { args -> remotePackages = args[0] }
      remotePackages.size() == 1
      remotePackages.containsKey(coordinate) == true
      remotePackages[coordinate].size() == 1
      remotePackages[coordinate][0] instanceof RemoteMavenAsset
  }

  @WithoutJenkins
  def 'it fails if asset does not exist'() {
    setup:
      run.getEnvironment(_) >> [:]
      def spyUploader = Spy(getUploader().class, constructorArgs: [Mock(NxrmConfiguration), run, taskListener])
      def coordinate = Mock(MavenCoordinate)
      def workspace = File.createTempDir()
      workspace.deleteOnExit()
      def publisher = Mock(NexusPublisher)
      publisher.packages >> [
          new MavenPackage(coordinate, [new MavenAsset('does-not-exist', 'classifier', 'extension')])
      ]

    when:
      spyUploader.uploadComponents(publisher, new FilePath(workspace))
      workspace.delete()

    then:
      def thrown = thrown(IOException)
      thrown.message == 'does-not-exist does not exist'
      1 * run.setResult(Result.FAILURE)
  }

  @WithoutJenkins
  def 'it expands the file path'() {
    def tempFile = File.createTempFile("temp", ".tmp")
    tempFile.deleteOnExit()
    def workspace = new FilePath(tempFile.getParentFile())
    run.getEnvironment(_) >> new EnvVars([ 'PATH': tempFile.getAbsolutePath()])
    def spyUploader = Spy(getUploader().class, constructorArgs: [Mock(NxrmConfiguration), run, taskListener])
    def coordinate = Mock(MavenCoordinate)
    def publisher = Mock(NexusPublisher)
    publisher.packages >> [
        new MavenPackage(coordinate, [new MavenAsset('$PATH', 'classifier', 'extension')])
    ]

    def remotePackages = null

    when:
      spyUploader.uploadComponents(publisher, workspace)
      tempFile.delete()

    then:
      1 * spyUploader.upload(*_) >> { args -> remotePackages = args[0] }
      remotePackages.size() == 1
      remotePackages[coordinate][0].RemotePath.toString() == tempFile.getAbsolutePath()
  }

  class SimplePackage
      extends Package
  {
    SimplePackage(Coordinate coordinate, List<Asset> assets) {
      this.coordinate = coordinate
      this.assets = assets
    }

    final class DescriptorImpl
        extends Package.PackageDescriptor
    {
      DescriptorImpl() {
        super(SimplePackage)
      }

      @Override
      String getDisplayName() {
        return 'Simple Package'
      }
    }
  }
}
