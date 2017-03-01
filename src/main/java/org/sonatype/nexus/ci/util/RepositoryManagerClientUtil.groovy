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

import com.sonatype.nexus.api.common.Authentication
import com.sonatype.nexus.api.common.ServerConfig
import com.sonatype.nexus.api.repository.RepositoryManagerClient
import com.sonatype.nexus.api.repository.RepositoryManagerClientBuilder

import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder
import hudson.security.ACL
import jenkins.model.Jenkins

@SuppressWarnings(value = ['AbcMetric'])
class RepositoryManagerClientUtil
{
  static RepositoryManagerClient newRepositoryManagerClient(String url, String credentialsId)
      throws URISyntaxException
  {
    def uri = new URI(url)

    def credentials = CredentialsMatchers.firstOrNull(CredentialsProvider
        .lookupCredentials(StandardUsernamePasswordCredentials, Jenkins.getInstance(), ACL.SYSTEM,
        URIRequirementBuilder.fromUri(url).build()), CredentialsMatchers.withId(credentialsId))

    def authentication

    if (credentialsId) {
      if (!credentials) {
        throw new IllegalArgumentException("No credentials were found for credentialsId: ${credentialsId}")
      }
      authentication = new Authentication(credentials.getUsername(), credentials.getPassword().getPlainText())
    }

    def serverConfig = authentication != null ? new ServerConfig(uri, authentication) : new ServerConfig(uri)

    def clientBuilder = RepositoryManagerClientBuilder.create().withServerConfig(serverConfig)

    def jenkinsProxy = Jenkins.instance.proxy

    if (jenkinsProxy && ProxyUtil.shouldProxyForUri(jenkinsProxy, uri)) {
      clientBuilder.withProxyConfig(ProxyUtil.newProxyConfig(jenkinsProxy))
    }

    return clientBuilder.build()
  }
}
