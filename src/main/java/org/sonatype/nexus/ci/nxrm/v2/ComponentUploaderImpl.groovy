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
package org.sonatype.nexus.ci.nxrm.v2

import com.sonatype.nexus.api.exception.RepositoryManagerException
import com.sonatype.nexus.api.repository.v2.GAV
import com.sonatype.nexus.api.repository.v2.MavenAsset
import com.sonatype.nexus.api.repository.v2.RepositoryManagerClient

import org.sonatype.nexus.ci.config.NxrmConfiguration
import org.sonatype.nexus.ci.nxrm.BaseComponentUploader
import org.sonatype.nexus.ci.nxrm.MavenCoordinate

import hudson.EnvVars
import hudson.FilePath

import static hudson.util.FormValidation.Kind.ERROR
import static org.sonatype.nexus.ci.util.FormUtil.validateUrl
import static org.sonatype.nexus.ci.util.RepositoryManagerClientUtil.nexus2Client

@SuppressWarnings(['CatchException', 'AbcMetric', 'MethodSize'])
class ComponentUploaderImpl
    extends BaseComponentUploader
{
  ComponentUploaderImpl(final NxrmConfiguration nexusConfig,
                        final FilePath baseDir,
                        final EnvVars environment,
                        final PrintStream logger)
  {
    super(nexusConfig, baseDir, environment, logger)
  }

  @Override
  void doUpload(final String repository, final Map<MavenCoordinate, List<RemoteMavenAsset>> components)
      throws RepositoryManagerException
  {
    RepositoryManagerClient nxrmClient = getRepositoryManagerClient(nexusConfig)

    components.each { component ->
      MavenCoordinate coordinate = component.key
      List<RemoteMavenAsset> remoteAssets = component.value

      remoteAssets.each { RemoteMavenAsset remoteAsset ->
        try {
          logger.println("Uploading Maven asset with groupId: ${coordinate.groupId} " +
              "artifactId: ${coordinate.artifactId} version: ${coordinate.version} " +
              "To repository: ${repository}")

          def localFile = File.createTempFile(remoteAsset.remotePath.getName(), 'tmp')
          remoteAsset.remotePath.copyTo(new FilePath(localFile))

          def mavenCoordinate = new GAV(environment.expand(coordinate.groupId),
              environment.expand(coordinate.artifactId),
              environment.expand(coordinate.version),
              environment.expand(coordinate.packaging))
          def mavenFile = new MavenAsset(localFile, environment.expand(remoteAsset.asset.extension),
              environment.expand(remoteAssets.asset.classifier))

          try {
            nxrmClient.uploadComponent(repository, mavenCoordinate, [mavenFile])
          }
          finally {
            localFile.delete()
          }
        }
        catch (RepositoryManagerException e) {
          final String uploadFailed = "Upload of ${remoteAsset.asset.filePath} failed"
          logger.println(uploadFailed)
          logger.println('Failing build due to failure to upload file to Nexus Repository Manager Publisher')
          throw new RepositoryManagerException(uploadFailed, e)
        }
      }
    }

    logger.println('Successfully Uploaded Maven Assets')
  }

  RepositoryManagerClient getRepositoryManagerClient(final NxrmConfiguration nexusConfiguration) {
    def nexusServerUrl = nexusConfiguration.serverUrl

    if (validateUrl(nexusServerUrl).kind == ERROR) {
      def message = "Nexus Server URL ${nexusServerUrl} is invalid."
      logger.println(message)
      logger.println('Failing build due to invalid Nexus Server URL')
      throw new IllegalArgumentException(message)
    }

    try {
      return nexus2Client(nexusConfiguration.serverUrl, nexusConfiguration.credentialsId)
    }
    catch (Exception e) {
      def message = 'Error creating RepositoryManagerClient'
      logger.println(message)
      logger.println('Failing build due to error creating RepositoryManagerClient')
      throw e
    }
  }
}
