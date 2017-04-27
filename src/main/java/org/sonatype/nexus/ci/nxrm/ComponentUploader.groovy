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
import com.sonatype.nexus.api.repository.GAV
import com.sonatype.nexus.api.repository.RepositoryManagerClient

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.NxrmConfiguration
import org.sonatype.nexus.ci.util.FormUtil
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import hudson.EnvVars
import hudson.FilePath
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import hudson.util.FormValidation.Kind

@SuppressWarnings(['CatchException', 'AbcMetric', 'MethodSize'])
class ComponentUploader
{
  private final Run run

  private final PrintStream logger

  private final EnvVars envVars

  ComponentUploader(final Run run,
                    final TaskListener taskListener)
  {
    this.run = run
    this.logger = taskListener.getLogger()
    this.envVars = run.getEnvironment(taskListener)
  }

  void uploadComponents(final NexusPublisher nexusPublisher,
                        final FilePath filePath)
  {

    def nexusConfiguration = getNexusConfiguration(nexusPublisher.nexusInstanceId)
    def nxrmClient = getRepositoryManagerClient(nexusConfiguration)
    def mavenPackages = getPackagesOfType(nexusPublisher.packages, MavenPackage)

    def remoteMavenComponents = []

    // Iterate through all packages and assets first to ensure that everything exists
    // This prevents uploading assets from an incomplete set
    mavenPackages.each { MavenPackage mavenPackage ->
      mavenPackage.assets.each { MavenAsset mavenFilePath ->
        def artifactPath = new FilePath(filePath, envVars.expand(mavenFilePath.filePath))

        if (!artifactPath.exists()) {
          final String missingFile = "${mavenFilePath.filePath} does not exist"

          logger.println(missingFile)
          logger.println('Failing build due to missing expected files for Nexus Repository Manager Publisher')
          run.setResult(Result.FAILURE)
          throw new IOException(missingFile)
        }

        remoteMavenComponents.add(new RemoteMavenComponent(mavenPackage.coordinate, mavenFilePath, artifactPath))
      }
    }

    remoteMavenComponents.each { RemoteMavenComponent component ->
      try {
        logger.println("Uploading Maven asset with groupId: ${component.Coordinate.groupId} " +
            "artifactId: ${component.Coordinate.artifactId} version: ${component.Coordinate.version} " +
            "To repository: ${nexusPublisher.nexusRepositoryId}")

        def localFile = File.createTempFile(component.RemotePath.getName(), 'tmp')
        component.RemotePath.copyTo(new FilePath(localFile))

        def mavenCoordinate = new GAV(envVars.expand(component.Coordinate.groupId),
            envVars.expand(component.Coordinate.artifactId), envVars.expand(component.Coordinate.version),
            envVars.expand(component.Coordinate.packaging))
        def mavenFile = new com.sonatype.nexus.api.repository.MavenAsset(localFile,
            envVars.expand(component.Asset.extension), envVars.expand(component.Asset.classifier))

        try {
          nxrmClient.uploadComponent(nexusPublisher.nexusRepositoryId, mavenCoordinate, [mavenFile])
        }
        catch (RepositoryManagerException ex) {
          throw new IOException(ex)
        }
        finally {
          localFile.delete()
        }
      }
      catch (IOException ex) {
        final String uploadFailed = "Upload of ${component.Asset.filePath} failed"

        logger.println(uploadFailed)
        logger.println('Failing build due to failure to upload file to Nexus Repository Manager Publisher')
        run.setResult(Result.FAILURE)
        throw new IOException(uploadFailed, ex)
      }
    }

    logger.println('Successfully Uploaded Maven Assets')
  }

  RepositoryManagerClient getRepositoryManagerClient(final NxrmConfiguration nexusConfiguration)
  {
    try {
      return RepositoryManagerClientUtil.newRepositoryManagerClient(nexusConfiguration.serverUrl,
          nexusConfiguration.credentialsId)
    }
    catch (Exception e) {
      def message = 'Error creating RepositoryManagerClient'
      logger.println(message)
      logger.println('Failing build due to error creating RepositoryManagerClient')
      run.setResult(Result.FAILURE)
      throw e
    }
  }

  NxrmConfiguration getNexusConfiguration(final String nexusInstanceId)
  {
    def nexusConfiguration = GlobalNexusConfiguration.globalNexusConfiguration.nxrmConfigs.find {
      return it.id == nexusInstanceId
    }

    if (!nexusConfiguration) {
      def message = "Nexus Configuration ${nexusInstanceId} not found."
      logger.println(message)
      logger.println('Failing build due to missing Nexus Configuration')
      run.setResult(Result.FAILURE)
      throw new IllegalArgumentException(message)
    }

    if (FormUtil.validateUrl(nexusConfiguration.serverUrl).kind == Kind.ERROR) {
      def message = "Nexus Server URL ${nexusConfiguration.serverUrl} is invalid."
      logger.println(message)
      logger.println('Failing build due to invalid Nexus Server URL')
      run.setResult(Result.FAILURE)
      throw new IllegalArgumentException(message)
    }

    return nexusConfiguration
  }

  @SuppressWarnings('Instanceof')
  private static <T extends Package> List<T> getPackagesOfType(List<Package> packageList, Class<T> type) {
    return packageList.findAll { Package iPackage ->
      return iPackage instanceof T
    }.collect { Package iPackage ->
      return type.cast(iPackage)
    }
  }

  @SuppressWarnings(['FieldName', 'PublicInstanceField'])
  private static class RemoteMavenComponent
  {
    public final MavenCoordinate Coordinate
    public final MavenAsset Asset
    public final FilePath RemotePath

    RemoteMavenComponent(final MavenCoordinate coordinate,
                         final MavenAsset asset,
                         final FilePath remotePath)
    {
      this.Coordinate = coordinate
      this.Asset = asset
      this.RemotePath = remotePath
    }
  }
}
