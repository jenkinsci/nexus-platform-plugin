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

import hudson.EnvVars
import hudson.FilePath
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener

@SuppressWarnings(['CatchException', 'AbcMetric', 'MethodSize'])
abstract class ComponentUploader
{
  protected final NxrmConfiguration nxrmConfiguration

  protected final Run run

  protected final PrintStream logger

  protected final EnvVars envVars

  protected ComponentUploader(final NxrmConfiguration nxrmConfiguration,
                              final Run run,
                              final TaskListener taskListener)
  {
    this.nxrmConfiguration = nxrmConfiguration
    this.run = run
    this.logger = taskListener.getLogger()
    this.envVars = run.getEnvironment(taskListener)
  }

  protected abstract void upload(final Map<MavenCoordinate, List<RemoteMavenAsset>> remoteMavenComponents,
                                 final String nxrmRepositoryId,
                                 final String tagName = null) throws IOException

  @SuppressWarnings(['UnusedMethodParameter', 'EmptyMethodInAbstractClass'])
  void maybeCreateTag(final String tagName) {
  }

  void uploadComponents(final NexusPublisher nexusPublisher,
                        final FilePath filePath,
                        final String tagName = null)
  {
    def mavenPackages = getPackagesOfType(nexusPublisher.packages, MavenPackage)
    def remoteMavenComponents = [:]

    maybeCreateTag(tagName)

    // Iterate through all packages and assets first to ensure that everything exists
    // This prevents uploading assets from an incomplete set
    mavenPackages.each { MavenPackage mavenPackage ->
      remoteMavenComponents.put(mavenPackage.coordinate, [])

      mavenPackage.assets.each { MavenAsset mavenAsset ->
        def artifactPath = new FilePath(filePath, envVars.expand(mavenAsset.filePath))

        if (!artifactPath.exists()) {
          final String missingFile = "${mavenAsset.filePath} does not exist"

          logger.println(missingFile)
          logger.println('Failing build due to missing expected files for Nexus Repository Manager Publisher')
          run.setResult(Result.FAILURE)
          throw new IOException(missingFile)
        }

        remoteMavenComponents[mavenPackage.coordinate].add(new RemoteMavenAsset(mavenAsset, artifactPath))
      }
    }

    try {
      upload(remoteMavenComponents, nexusPublisher.nexusRepositoryId, tagName)
    }
    catch (IOException ex) {
      logger.println("${ex.getMessage()} - cause: ${ex.getCause()}")
      logger.println('Failing build due to failure to upload file to Nexus Repository Manager Publisher')
      run.setResult(Result.FAILURE)
      throw ex
    }
  }

  @SuppressWarnings('Instanceof')
  private static <T extends Package> List<T> getPackagesOfType(List<Package> packageList, Class<T> type) {
    return packageList.findAll { it.class == type }.collect { type.cast(it) }
  }

  @SuppressWarnings(['FieldName', 'PublicInstanceField'])
  static class RemoteMavenAsset
  {
    public final MavenAsset Asset

    public final FilePath RemotePath

    RemoteMavenAsset(final MavenAsset asset, final FilePath remotePath) {
      this.Asset = asset
      this.RemotePath = remotePath
    }
  }
}
