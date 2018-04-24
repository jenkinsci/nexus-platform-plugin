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
import com.sonatype.nexus.api.repository.v2.RepositoryManagerV2Client

import org.sonatype.nexus.ci.config.Nxrm2Configuration
import org.sonatype.nexus.ci.config.NxrmConfiguration
import org.sonatype.nexus.ci.nxrm.ComponentUploader
import org.sonatype.nexus.ci.nxrm.MavenCoordinate

import groovy.transform.InheritConstructors
import groovy.transform.PackageScope
import hudson.FilePath
import hudson.model.Result

import static com.sonatype.nexus.api.common.ArgumentUtils.checkArgument
import static org.sonatype.nexus.ci.util.RepositoryManagerClientUtil.nexus2Client

@SuppressWarnings(['CatchException', 'AbcMetric', 'MethodSize'])
@InheritConstructors
class ComponentUploaderNxrm2
    extends ComponentUploader
{
  @Override
  void upload(final Map<MavenCoordinate, List<RemoteMavenAsset>> remoteMavenComponents,
              final String nxrmRepositoryId)
  {
    def nxrmClient = getRepositoryManagerClient(nxrmConfiguration)

    remoteMavenComponents.each { mavenCoordinate, remoteMavenAssets ->
      logger.println("Uploading Maven asset with groupId: ${mavenCoordinate.groupId} " +
          "artifactId: ${mavenCoordinate.artifactId} version: ${mavenCoordinate.version} " +
          "To repository: ${nxrmRepositoryId}")

      remoteMavenAssets.each { remoteMavenAsset ->
        try {
          def localFile = File.createTempFile(remoteMavenAsset.RemotePath.getName(), '.tmp')
          remoteMavenAsset.RemotePath.copyTo(new FilePath(localFile))

          def gav = new GAV(envVars.expand(mavenCoordinate.groupId), envVars.expand(mavenCoordinate.artifactId),
              envVars.expand(mavenCoordinate.version), envVars.expand(mavenCoordinate.packaging))

          def mavenFile = new MavenAsset(localFile, envVars.expand(remoteMavenAsset.Asset.extension),
              envVars.expand(remoteMavenAsset.Asset.classifier))

          try {
            nxrmClient.uploadComponent(nxrmRepositoryId, gav, [mavenFile])
          }
          catch (RepositoryManagerException ex) {
            throw new IOException(ex)
          }
          finally {
            localFile.delete()
          }
        }
        catch (IOException ex) {
          final String uploadFailed = "Upload of ${remoteMavenAsset.Asset.filePath} failed"

          logger.println(uploadFailed)
          logger.println('Failing build due to failure to upload file to Nexus Repository Manager Publisher')
          run.setResult(Result.FAILURE)
          throw new IOException(uploadFailed, ex)
        }
      }

      logger.println('Successfully Uploaded Maven Assets')
    }
  }

  @PackageScope
  RepositoryManagerV2Client getRepositoryManagerClient(final NxrmConfiguration nexusConfiguration) {
    try {
      checkArgument(nxrmConfiguration.class == Nxrm2Configuration.class,
          'Nexus Repository Manager 2.x server is required')
      return nexus2Client(nexusConfiguration.serverUrl, nexusConfiguration.credentialsId)
    }
    catch (Exception e) {
      logger.println('Failing build due to error creating RepositoryManagerClient')
      run.setResult(Result.FAILURE)
      throw e
    }
  }
}
