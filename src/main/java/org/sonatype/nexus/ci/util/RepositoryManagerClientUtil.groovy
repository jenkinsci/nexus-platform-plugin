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
package org.sonatype.nexus.ci.util

import javax.annotation.Nullable

import com.sonatype.nexus.api.common.Authentication
import com.sonatype.nexus.api.common.ServerConfig
import com.sonatype.nexus.api.exception.RepositoryManagerException
import com.sonatype.nexus.api.repository.v2.RepositoryManagerV2Client
import com.sonatype.nexus.api.repository.v2.RepositoryManagerV2ClientBuilder
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3ClientBuilder

import org.sonatype.nexus.ci.config.Nxrm3Configuration
import org.sonatype.nexus.ci.config.NxrmVersion

import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder
import hudson.security.ACL
import hudson.util.FormValidation.Kind
import jenkins.model.Jenkins

import static org.sonatype.nexus.ci.config.NxrmVersion.NEXUS_3
import static org.sonatype.nexus.ci.util.FormUtil.validateUrl

@SuppressWarnings(value = ['AbcMetric'])
class RepositoryManagerClientUtil
{
  static RepositoryManagerV2Client nexus2Client(String url, String credentialsId)
      throws URISyntaxException
  {
    def uri = new URI(url)
    def clientBuilder = RepositoryManagerV2ClientBuilder.create().
        withServerConfig(getServerConfig(uri, credentialsId))
    def jenkinsProxy = Jenkins.instance.proxy

    if (jenkinsProxy && ProxyUtil.shouldProxyForUri(jenkinsProxy, uri)) {
      clientBuilder.withProxyConfig(ProxyUtil.newProxyConfig(jenkinsProxy))
    }

    return clientBuilder.build()
  }

  /**
   * Creates a NXRM3 client using the specified Jenkins Nexus RM instance
   *
   * @param nexusInstanceId the configured NXRM3 server in Jenkins
   * @return a {@link RepositoryManagerV3Client}
   * @throws RepositoryManagerException if the {@link Nxrm3Configuration} cannot be found, or is incorrectly configured
   */
  static RepositoryManagerV3Client nexus3Client(String nexusInstanceId) throws RepositoryManagerException {
    def nxrmConfig = NxrmUtil.getNexusConfiguration(nexusInstanceId)

    if (!nxrmConfig) {
      throw new RepositoryManagerException("Nexus Configuration ${nexusInstanceId} not found.")
    }

    if (nxrmConfig.version != NEXUS_3) {
      throw new RepositoryManagerException("The specified instance is not a Nexus Repository Manager 3 server")
    }

    if (validateUrl(nxrmConfig.serverUrl).kind == Kind.ERROR) {
      throw new RepositoryManagerException("Nexus Server URL ${nxrmConfig.serverUrl} is invalid.")
    }

    return nexus3Client(nxrmConfig.serverUrl, nxrmConfig.credentialsId)
  }

  /**
   * Creates a nxrm 3.x client
   * @param url the nexus repository manager 3.x server url
   * @param credentialsId the id of the credentials configured in jenkisn
   * @return a {@link RepositoryManagerV3Client}
   */
  static RepositoryManagerV3Client nexus3Client(String url, @Nullable String credentialsId)
      throws URISyntaxException
  {
    def uri = new URI(url)
    def clientBuilder = RepositoryManagerV3ClientBuilder.create().
        withServerConfig(getServerConfig(uri, credentialsId))
    def jenkinsProxy = Jenkins.instance.proxy

    if (jenkinsProxy && ProxyUtil.shouldProxyForUri(jenkinsProxy, uri)) {
      clientBuilder.withProxyConfig(ProxyUtil.newProxyConfig(jenkinsProxy))
    }

    return clientBuilder.build()
  }

  private static ServerConfig getServerConfig(URI uri, String credentialsId) {
    def credentials = CredentialsMatchers.firstOrNull(CredentialsProvider
        .lookupCredentials(StandardUsernamePasswordCredentials, Jenkins.getInstance(), ACL.SYSTEM,
        URIRequirementBuilder.fromUri(uri.toString()).build()), CredentialsMatchers.withId(credentialsId))

    def authentication

    if (credentialsId) {
      if (!credentials) {
        throw new IllegalArgumentException("No credentials were found for credentialsId: ${credentialsId}")
      }
      authentication = new Authentication(credentials.getUsername(), credentials.getPassword().getPlainText())
    }

    return authentication ? new ServerConfig(uri, authentication) : new ServerConfig(uri)
  }
}
