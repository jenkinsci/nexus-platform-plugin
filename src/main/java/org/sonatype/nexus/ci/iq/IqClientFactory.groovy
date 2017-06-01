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

import com.cloudbees.plugins.credentials.Credentials
import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials
import com.cloudbees.plugins.credentials.common.StandardCredentials
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder
import hudson.model.Item
import hudson.model.ItemGroup
import hudson.security.ACL
import jenkins.model.Jenkins
import org.slf4j.Logger

class IqClientFactory
{
  static InternalIqClient getIqTestClient() {
    return getIqTestClient(NxiqConfiguration.serverUrl, NxiqConfiguration.credentialsId, Jenkins.instance)
  }

  static InternalIqClient getIqClient(String credentialsId, Item context) {
    // credentialsId can be an empty String if unset. Empty strings are falsy in Groovy
    def credentials = credentialsId ? findCredentials(NxiqConfiguration.serverUrl, credentialsId, context)
      : findCredentials(NxiqConfiguration.serverUrl, NxiqConfiguration.credentialsId, Jenkins.instance)

    return createIqClient(NxiqConfiguration.serverUrl, credentials)
  }

  static InternalIqClient getIqTestClient(URI serverUrl, @Nullable String credentialsId, ItemGroup context) {
    def credentials = findCredentials(serverUrl, credentialsId, context)
    return createIqClient(serverUrl, credentials)
  }

  static InternalIqClient getIqClient(@Nullable Logger log, ItemGroup context, String credentialsId) {
    def credentials = findCredentials(NxiqConfiguration.serverUrl, credentialsId, context)
    return createIqClient(NxiqConfiguration.serverUrl, credentials, log)
  }

  private static InternalIqClient createIqClient(URI serverUrl, Credentials credentials, Logger log = null) {
    def serverConfig = createServerConfig(serverUrl, credentials)
    def proxyConfig = getProxyConfig(serverUrl)
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

  static ProxyConfig getProxyConfig(URI url) {
    def jenkinsProxy = Jenkins.instance.proxy

    if (jenkinsProxy && ProxyUtil.shouldProxyForUri(jenkinsProxy, url)) {
      return ProxyUtil.newProxyConfig(jenkinsProxy)
    } else {
      return null
    }
  }

  static private StandardCredentials findCredentials(final URI url, final String credentialsId, final context) {
    def lookupCredentials
    if (context instanceof Item) {
      lookupCredentials = CredentialsProvider.lookupCredentials(
          StandardCredentials,
          context,
          ACL.SYSTEM,
          URIRequirementBuilder.fromUri(url.toString()).build())
    } else if (context instanceof ItemGroup) {
      lookupCredentials = CredentialsProvider.lookupCredentials(
          StandardCredentials,
          context,
          ACL.SYSTEM,
          URIRequirementBuilder.fromUri(url.toString()).build())
    } else {
      throw new IllegalArgumentException(Messages.IqClientFactory_NoCredentials(credentialsId))
    }
    StandardCredentials credentials = CredentialsMatchers.firstOrNull(lookupCredentials, CredentialsMatchers.withId(credentialsId))
    if (!credentials) {
      throw new IllegalArgumentException(Messages.IqClientFactory_NoCredentials(credentialsId))
    } else {
      return credentials
    }
  }

  static private ServerConfig createServerConfig(final URI url, final credentials) {
    if (credentials instanceof StandardUsernamePasswordCredentials) {
      return new ServerConfig(url, new Authentication(credentials.getUsername(), credentials.getPassword().getPlainText()))
    } else if (credentials instanceof StandardCertificateCredentials) {
      def aliases = credentials.getKeyStore().aliases().toList()
      credentials.getKeyStore().getKey(aliases[0], credentials.password.plainText.toCharArray())
      return new ServerConfig(url, new PkiAuthentication(credentials.getKeyStore(), credentials.password.plainText.toCharArray()))
    } else {
      throw new IllegalArgumentException(Messages.IqClientFactory_UnsupportedCredentials(credentials.class))
    }
  }
}
