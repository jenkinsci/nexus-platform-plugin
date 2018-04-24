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
package org.sonatype.nexus.ci.nxrm.v3

import com.sonatype.nexus.api.exception.RepositoryManagerException
import com.sonatype.nexus.api.repository.v3.DefaultAsset
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client
import com.sonatype.nexus.api.repository.v3.formats.maven.MavenComponentBuilder

import org.sonatype.nexus.ci.config.Nxrm3Configuration
import org.sonatype.nexus.ci.config.NxrmConfiguration
import org.sonatype.nexus.ci.nxrm.ComponentUploader
import org.sonatype.nexus.ci.nxrm.MavenCoordinate

import groovy.transform.InheritConstructors
import groovy.transform.PackageScope
import hudson.model.Result

import static com.sonatype.nexus.api.common.ArgumentUtils.checkArgument
import static org.sonatype.nexus.ci.util.RepositoryManagerClientUtil.nexus3Client

@SuppressWarnings(['CatchException', 'AbcMetric', 'MethodSize'])
@InheritConstructors
class ComponentUploaderNxrm3
    extends ComponentUploader
{
  @Override
  void upload(final Map<MavenCoordinate, List<RemoteMavenAsset>> remoteMavenComponents,
              final String nxrmRepositoryId)
  {
    def nxrmClient = getRepositoryManagerClient(nxrmConfiguration)

    remoteMavenComponents.each { mavenCoordinate, remoteMavenAssets ->
      try {
        logger.println("Uploading Maven asset with groupId: ${mavenCoordinate.groupId} " +
            "artifactId: ${mavenCoordinate.artifactId} version: ${mavenCoordinate.version} " +
            "To repository: ${nxrmRepositoryId}")

        def mavenComponentBuilder = MavenComponentBuilder.create()
            .withGroupId(envVars.expand(mavenCoordinate.groupId))
            .withArtifactId(envVars.expand(mavenCoordinate.artifactId))
            .withVersion(envVars.expand(mavenCoordinate.version))
            .withPackaging(envVars.expand(mavenCoordinate.packaging))

        remoteMavenAssets.eachWithIndex { remoteMavenAsset, idx ->
          def asset = new DefaultAsset("asset${idx}", remoteMavenAsset.RemotePath.getRemote(),
              remoteMavenAsset.RemotePath.read())

          mavenComponentBuilder.withAsset(asset, envVars.expand(remoteMavenAsset.Asset.extension),
              envVars.expand(remoteMavenAsset.Asset.classifier))
        }

        try {
          nxrmClient.upload(nxrmRepositoryId, mavenComponentBuilder.build())
        }
        catch (RepositoryManagerException ex) {
          throw new IOException(ex)
        }
      }
      catch (IOException ex) {
        final String uploadFailed = 'Upload of maven component with GAV ' +
            "[${mavenCoordinate.groupId}:${mavenCoordinate.artifactId}:${mavenCoordinate.version}] failed"

        logger.println(uploadFailed)
        logger.println('Failing build due to failure to upload file to Nexus Repository Manager Publisher')
        run.setResult(Result.FAILURE)
        throw new IOException(uploadFailed, ex)
      }
    }

    logger.println('Successfully Uploaded Maven Assets')
  }

  @PackageScope
  RepositoryManagerV3Client getRepositoryManagerClient(final NxrmConfiguration nxrmConfiguration) {
    try {
      checkArgument(nxrmConfiguration.class == Nxrm3Configuration.class,
          'Nexus Repository Manager 3.x server is required')
      Nxrm3Configuration nxrm3Configuration = nxrmConfiguration as Nxrm3Configuration
      nexus3Client(nxrm3Configuration.serverUrl, nxrm3Configuration.credentialsId, nxrm3Configuration.anonymousAccess)
    }
    catch (Exception e) {
      logger.println('Error creating RepositoryManagerClient')
      logger.println('Failing build due to error creating RepositoryManagerClient')
      run.setResult(Result.FAILURE)
      throw e
    }
  }
}
