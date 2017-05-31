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
package org.sonatype.nexus.ci.iq

import javax.annotation.Nullable

import com.sonatype.nexus.api.common.Authentication
import com.sonatype.nexus.api.common.PkiAuthentication
import com.sonatype.nexus.api.common.ProxyConfig
import com.sonatype.nexus.api.common.ServerConfig
import com.sonatype.nexus.api.iq.internal.InternalIqClient
import com.sonatype.nexus.api.iq.internal.InternalIqClientBuilder

import org.sonatype.nexus.ci.config.NxiqConfiguration
import org.sonatype.nexus.ci.util.ProxyUtil

import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials
import com.cloudbees.plugins.credentials.common.StandardCredentials
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder
import hudson.model.ItemGroup
import hudson.security.ACL
import org.slf4j.Logger
import jenkins.model.Jenkins

class IqClientFactory
{
  static InternalIqClient getIqClient() {
    return getIqClient(NxiqConfiguration.serverUrl, (String) NxiqConfiguration.credentialsId)
  }

  static InternalIqClient getIqClient(String credentialsId) {
    // credentialsId can be an empty String if unset. Empty strings are falsy in Groovy
    return getIqClient(NxiqConfiguration.serverUrl, credentialsId ?: NxiqConfiguration.credentialsId)
  }

  static InternalIqClient getIqClient(URI serverUrl, @Nullable String credentialsId) {
    return (InternalIqClient) InternalIqClientBuilder.create()
        .withServerConfig(getServerConfig(serverUrl, credentialsId, Jenkins.instance))
        .withProxyConfig(getProxyConfig(serverUrl))
        .build()
  }

  static InternalIqClient getIqClient(Logger log, ItemGroup project, @Nullable String credentialsId) {
    def serverConfig = getServerConfig(NxiqConfiguration.serverUrl, credentialsId ?: NxiqConfiguration.credentialsId, project)
    def proxyConfig = getProxyConfig(NxiqConfiguration.serverUrl)
    return (InternalIqClient) InternalIqClientBuilder.create()
        .withServerConfig(serverConfig)
        .withProxyConfig(proxyConfig)
        .withLogger(log)
        .build()
  }

  static InternalIqClient getIqLocalClient(Logger log, String instanceId) {
    return (InternalIqClient) InternalIqClientBuilder.create()
        .withInstanceId(instanceId)
        .withLogger(log)
        .build()
  }

  private static ServerConfig getServerConfig(URI url, @Nullable String credentialsId, @Nullable final ItemGroup project) {
    if (credentialsId) {
      return loadCredentials(url, credentialsId, project)
    }
    else {
      return new ServerConfig(url)
    }
  }

  static ProxyConfig getProxyConfig(URI url) {
    def jenkinsProxy = Jenkins.instance.proxy

    if (jenkinsProxy && ProxyUtil.shouldProxyForUri(jenkinsProxy, url)) {
      return ProxyUtil.newProxyConfig(jenkinsProxy)
    } else {
      return null
    }
  }

  static private ServerConfig loadCredentials(final URI url, final String credentialsId, @Nullable ItemGroup project) {
    def lookupCredentials = CredentialsProvider.lookupCredentials(
        StandardCredentials,
        project ? project : Jenkins.instance,
        ACL.SYSTEM,
        URIRequirementBuilder.fromUri(url.toString()).build())

    def credentials = CredentialsMatchers.firstOrNull(lookupCredentials, CredentialsMatchers.withId(credentialsId))
    if (!credentials) {
      throw new IllegalArgumentException(Messages.IqClientFactory_NoCredentials(credentialsId))
    }

    if (credentials instanceof StandardUsernamePasswordCredentials) {
      return new ServerConfig(url, new Authentication(credentials.getUsername(), credentials.getPassword().getPlainText()))
    } else if (credentials instanceof StandardCertificateCredentials) {
      def aliases = credentials.getKeyStore().aliases().toList()
      credentials.getKeyStore().getKey(aliases[0], credentials.password.plainText.toCharArray())
      return new ServerConfig(url, new PkiAuthentication(credentials.getKeyStore(), credentials.password.plainText.toCharArray()))

    } else {
      throw new IllegalArgumentException(Messages.IqClientFactory_NoCredentials(credentialsId))
    }
  }
}
