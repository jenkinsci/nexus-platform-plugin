package org.sonatype.nexus.ci.nxrm

import com.sonatype.nexus.api.exception.RepositoryManagerException

import org.sonatype.nexus.ci.config.Nxrm2Configuration

import hudson.EnvVars
import hudson.FilePath

import static com.google.common.base.Preconditions.checkNotNull

abstract class BaseComponentUploader
    implements ComponentUploader
{
  protected final Nxrm2Configuration nexusConfig

  protected final FilePath baseDir

  protected final EnvVars environment

  protected final PrintStream logger

  protected BaseComponentUploader(final Nxrm2Configuration nexusConfig,
                                  final FilePath baseDir,
                                  final EnvVars environment,
                                  final PrintStream logger)
  {
    this.nexusConfig = checkNotNull(nexusConfig)
    this.baseDir = baseDir
    this.environment = environment
    this.logger = logger
  }

  @Override
  void uploadComponents(final String repository,
                        final List<Package> packages)
      throws RepositoryManagerException
  {
    def mavenPackages = getPackagesOfType(packages, MavenPackage)
    def remoteMavenComponents = [:].withDefault { [] }

    // Iterate through all packages and assets first to ensure that everything exists
    // This prevents uploading assets from an incomplete set
    mavenPackages.each { MavenPackage mavenPackage ->
      mavenPackage.assets.each { MavenAsset mavenFilePath ->
        def artifactPath = new FilePath(baseDir, environment.expand(mavenFilePath.filePath))

        if (!artifactPath.exists()) {
          final String missingFile = "${mavenFilePath.filePath} does not exist"

          logger.println(missingFile)
          logger.println('Failing build due to missing expected files for Nexus Repository Manager Publisher')
          throw new IOException(missingFile)
        }

        remoteMavenComponents[mavenPackage.coordinate].
            add(new RemoteMavenAsset(mavenFilePath, artifactPath))
      }
    }

    doUpload(repository, remoteMavenComponents)

    logger.println('Successfully Uploaded Maven Assets')
  }

  abstract void doUpload(String repository, Map<MavenCoordinate, List<RemoteMavenAsset>> components)
      throws RepositoryManagerException

  @SuppressWarnings('Instanceof')
  private static <T extends Package> List<T> getPackagesOfType(List<Package> packageList, Class<T> type) {
    return packageList.findAll { Package iPackage ->
      return iPackage instanceof T
    }.collect { Package iPackage ->
      return type.cast(iPackage)
    }
  }

  @SuppressWarnings(['FieldName', 'PublicInstanceField'])
  static class RemoteMavenAsset
  {
    final MavenAsset asset

    final FilePath remotePath

    RemoteMavenAsset(final MavenAsset asset,
                     final FilePath remotePath)
    {
      this.asset = asset
      this.remotePath = remotePath
    }
  }
}
