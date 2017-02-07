/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm

import com.sonatype.nexus.api.exception.RepositoryManagerException
import com.sonatype.nexus.api.repository.GAV
import com.sonatype.nexus.api.repository.RepositoryManagerClient
import com.sonatype.nexus.ci.config.GlobalNexusConfiguration
import com.sonatype.nexus.ci.util.FormUtil
import com.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import hudson.EnvVars
import hudson.FilePath
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import hudson.remoting.VirtualChannel
import jenkins.MasterToSlaveFileCallable

// TODO ignore for existing code. exceptions are only logged and then rethrown. refactor to reduce complexity
@SuppressWarnings(value=['CatchException', 'AbcMetric', 'MethodSize'])
class PackageUploaderUtil
{
  static void uploadPackage(final NexusPublisher nxrmPublisher, final Run run,
                                   final TaskListener taskListener, final FilePath filePath)
      throws IOException, InterruptedException
  {
    PrintStream logger = taskListener.getLogger()
    EnvVars envVars = run.getEnvironment(taskListener)

    def nexusConfiguration = GlobalNexusConfiguration.globalNexusConfiguration.nxrmConfigs.find {
      return it.id == nxrmPublisher.nexusInstanceId
    }

    if (!nexusConfiguration) {
      def message = "Nexus Configuration ${nxrmPublisher.nexusInstanceId} not found."
      logger.println(message)
      logger.println('Failing build due to missing Nexus Configuration')
      run.setResult(Result.FAILURE)
      throw new IllegalArgumentException(message)
    }

    if (!FormUtil.validateUrl(nexusConfiguration.serverUrl)) {
      def message = "Nexus Server URL ${nexusConfiguration.serverUrl} is invalid."
      logger.println(message)
      logger.println('Failing build due to invalid Nexus Server URL')
      run.setResult(Result.FAILURE)
      throw new IllegalArgumentException(message)
    }

    def nxrmClient

    try {
      nxrmClient = RepositoryManagerClientUtil.buildRmClient(nexusConfiguration.serverUrl,
          nexusConfiguration.credentialsId)
    }
    catch (Exception e) {
      def message = 'Error creating RepositoryManagerClient'
      logger.println(message)
      logger.println('Failing build due to error creating RepositoryManagerClient')
      run.setResult(Result.FAILURE)
      throw e
    }

    List<Closure> uploadCallableClosures = []

    def mavenPackages = getPackagesOfType(nxrmPublisher.packages, MavenPackage)

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

        def uploaderCallableClosure = { ->
          try {
            logger.println("Uploading Maven asset with groupId: ${mavenPackage.coordinate.groupId} " +
                "artifactId: ${mavenPackage.coordinate.artifactId} version: ${mavenPackage.coordinate.version} " +
                "To repository: ${nxrmPublisher.nexusRepositoryId}")
            def uploaderCallable = new MavenAssetUploaderCallable(nxrmClient, nxrmPublisher.nexusRepositoryId,
                mavenPackage.coordinate, mavenFilePath, envVars)
            artifactPath.act(uploaderCallable)
          }
          catch (IOException ex) {
            final String uploadFailed = "Upload of ${mavenFilePath.filePath} failed"

            logger.println(uploadFailed)
            logger.println('Failing build due to failure to upload file to Nexus Repository Manager Publisher')
            run.setResult(Result.FAILURE)
            throw new IOException(uploadFailed, ex)
          }
        }
        uploadCallableClosures.push(uploaderCallableClosure)
      }
    }

    // Perform uploads after verifying that all assets exist
    uploadCallableClosures.each { Closure uploadCallableClosure ->
      uploadCallableClosure()
    }

    logger.println('Successfully Uploaded Maven Assets')
  }

  @SuppressWarnings('Instanceof') // TODO warning ignored for existing code. refactor when convenient
  private static <T extends Package> List<T> getPackagesOfType(List<Package> packageList, Class<T> type) {
    return packageList.findAll { Package iPackage ->
      return iPackage instanceof T
    }.collect { Package iPackage ->
      return type.cast(iPackage)
    }
  }

  /**
   * Uploads the Maven asset on the slave where the {@link FilePath} is local.
   *
   * <strong>Warning:</code> implementations must be serializable, so prefer a static nested class to an inner class.
   */
  @SuppressWarnings('UnnecessaryTransientModifier')  // TODO confirm if transient is used by jenkins or framework ?
  static class MavenAssetUploaderCallable
      extends MasterToSlaveFileCallable<Void>
  {
    private final transient RepositoryManagerClient client

    private final String repositoryId

    private final transient MavenCoordinate coordinate

    private final transient MavenAsset mavenAsset

    private final transient EnvVars envVars

    MavenAssetUploaderCallable(final RepositoryManagerClient client, final String repositoryId,
                               final MavenCoordinate coordinate, final MavenAsset mavenAsset, final EnvVars envVars)
    {
      this.client = client
      this.repositoryId = repositoryId
      this.coordinate = coordinate
      this.mavenAsset = mavenAsset
      this.envVars = envVars
    }

    @Override
    Void invoke(final File file, final VirtualChannel channel) throws IOException, InterruptedException {
      def mavenCoordinate = new GAV(envVars.expand(coordinate.groupId), envVars.expand(coordinate.artifactId),
          envVars.expand(coordinate.version), envVars.expand(coordinate.packaging))
      def mavenFile = new com.sonatype.nexus.api.repository.MavenAsset(file, envVars.expand(mavenAsset.extension),
          envVars.expand(mavenAsset.classifier))

      try {
        client.uploadComponent(repositoryId, mavenCoordinate, [mavenFile])
      }
      catch (RepositoryManagerException ex) {
        throw new IOException(ex)
      }
    }
  }
}
