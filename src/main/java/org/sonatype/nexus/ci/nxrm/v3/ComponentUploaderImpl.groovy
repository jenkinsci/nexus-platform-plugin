package org.sonatype.nexus.ci.nxrm.v3

import com.sonatype.nexus.api.exception.RepositoryManagerException
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client
import com.sonatype.nexus.api.repository.v3.formats.maven.MavenAsset
import com.sonatype.nexus.api.repository.v3.formats.maven.MavenUploadBuilder

import org.sonatype.nexus.ci.config.NxrmConfiguration
import org.sonatype.nexus.ci.nxrm.BaseComponentUploader
import org.sonatype.nexus.ci.nxrm.MavenCoordinate

import hudson.EnvVars
import hudson.FilePath

import static java.io.File.createTempFile
import static org.sonatype.nexus.ci.util.RepositoryManagerClientUtil.nexus3Client

@SuppressWarnings(['CatchThrowable', 'AbcMetric', 'MethodSize'])
class ComponentUploaderImpl
    extends BaseComponentUploader
{
  ComponentUploaderImpl(final NxrmConfiguration nexusConfig, final FilePath baseDir,
                        final EnvVars environment, final PrintStream logger)
  {
    super(nexusConfig, baseDir, environment, logger)
  }

  @Override
  void doUpload(final String repository, final Map<MavenCoordinate, List<RemoteMavenAsset>> components)
      throws RepositoryManagerException
  {
    RepositoryManagerV3Client nx3Client = null
    try {
      nx3Client = nexus3Client(nexusConfig.serverUrl, nexusConfig.credentialsId)
    }
    catch (Throwable t) {
      logger.println('Failing build due to error creating RepositoryManagerClient')
      throw t
    }

    components.each { uploadComponent(nx3Client, repository, it.key, it.value) }
  }

  private void uploadComponent(RepositoryManagerV3Client nx3Client,
                               String repository,
                               MavenCoordinate coordinate,
                               List<RemoteMavenAsset> remoteMavenAssets)
      throws RepositoryManagerException
  {
    def groupId = coordinate.groupId?.trim() ? environment.expand(coordinate.groupId) : null
    def artifactId = coordinate.artifactId?.trim() ? environment.expand(coordinate.artifactId) : null
    def version = coordinate.version?.trim() ? environment.expand(coordinate.version) : null
    def localFiles = []
    def uploadBuilder = MavenUploadBuilder.create()

    uploadBuilder.withGroupId(groupId)
    uploadBuilder.withArtifactId(artifactId)
    uploadBuilder.withVersion(version)
    if (coordinate.packaging?.trim()) {
      uploadBuilder.withPackaging(environment.expand(coordinate.packaging))
    }

    remoteMavenAssets.each { remoteAsset ->
      def mavenAsset = remoteAsset.asset
      def extension = mavenAsset.extension?.trim() ? environment.expand(mavenAsset.extension) : null
      def classifier = mavenAsset.classifier?.trim() ? environment.expand(mavenAsset.classifier) : null
      def localFile = createTempFile(remoteAsset.remotePath.name, 'tmp')
      remoteAsset.remotePath.copyTo(new FilePath(localFile))
      localFiles.add(localFile)

      switch (extension) {
        case 'pom':
          uploadBuilder.withPom(localFile.absolutePath, classifier)
          break
        case 'jar':
        case '':
        case null:
          uploadBuilder.withJar(localFile.absolutePath, classifier)
          break
        default:
          uploadBuilder.
              withAsset(new MavenAsset(localFile.name, new FileInputStream(localFile.absolutePath), extension))
          break
      }
    }

    try {
      def mavenUpload

      try {
        mavenUpload = uploadBuilder.build()
      }
      catch (Throwable t) {
        logger.println('Failing build due to invalid upload configuration')
        throw new RepositoryManagerException('Invalid upload configuration', t)
      }

      logger.println()
      logger.println("Uploading maven component with coordinates: ${coordinate}, and assets:")
      remoteMavenAssets.each { logger.println("--${it.asset}") }
      logger.println("to repository: ${repository}")
      logger.println()

      try {
        nx3Client.uploadComponent(repository, mavenUpload)
        logger.println("Successfully uploaded ${remoteMavenAssets.size()} assets")
        logger.println()
      }
      catch (Throwable t) {
        def message = "Upload of ${coordinate} failed"
        logger.println(message)
        logger.println('Failing build due to failure to upload component to Nexus Repository Manager Publisher')
        throw (t.class == RepositoryManagerException) ? t : new RepositoryManagerException(message, t)
      }
    }
    finally {
      localFiles.each { it.delete() }
    }
  }
}
