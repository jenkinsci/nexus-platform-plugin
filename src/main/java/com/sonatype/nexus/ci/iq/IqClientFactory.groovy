/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import javax.annotation.Nullable

import com.sonatype.nexus.api.common.Authentication
import com.sonatype.nexus.api.common.ProxyConfig
import com.sonatype.nexus.api.common.ServerConfig
import com.sonatype.nexus.api.iq.internal.InternalIqClient
import com.sonatype.nexus.api.iq.internal.InternalIqClientBuilder
import com.sonatype.nexus.ci.config.NxiqConfiguration
import com.sonatype.nexus.ci.util.ProxyUtil

import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.CredentialsProvider
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
    //TODO probably need to add proxy support
    // credentialsId can be an empty String if unset. Empty strings are falsy in Groovy
    return getIqClient(NxiqConfiguration.serverUrl, credentialsId ?: NxiqConfiguration.credentialsId)
  }

  static InternalIqClient getIqClient(URI serverUrl, @Nullable String credentialsId) {
    return (InternalIqClient) InternalIqClientBuilder.create()
        .withServerConfig(getServerConfig(serverUrl, credentialsId))
        .withProxyConfig(getProxyConfig(serverUrl))
        .build()
  }

  static InternalIqClient getIqClient(Logger log, @Nullable String credentialsId) {
    def serverConfig = getServerConfig(NxiqConfiguration.serverUrl, credentialsId ?: NxiqConfiguration.credentialsId)
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

  private static ServerConfig getServerConfig(URI url, @Nullable String credentialsId) {
    if (credentialsId) {
      def authentication = loadCredentials(url, credentialsId)
      return new ServerConfig(url, authentication)
    }
    else {
      return new ServerConfig(url)
    }
  }

  static ProxyConfig getProxyConfig(URI url) {
    def jenkinsProxy = Jenkins.instance.proxy

    if (jenkinsProxy && ProxyUtil.shouldProxyForUri(jenkinsProxy, url)) {
      return ProxyUtil.buildProxyConfig(jenkinsProxy)
    } else {
      return null
    }
  }

  static private Authentication loadCredentials(final URI url, final String credentialsId) {
    def lookupCredentials = CredentialsProvider.lookupCredentials(
        StandardUsernamePasswordCredentials,
        (ItemGroup) Jenkins.getInstance(),
        ACL.SYSTEM,
        URIRequirementBuilder.fromUri(url.toString()).build())

    def credentials = CredentialsMatchers.firstOrNull(lookupCredentials, CredentialsMatchers.withId(credentialsId))
    if (!credentials) {
      throw new IllegalArgumentException(Messages.IqClientFactory_NoCredentials(credentialsId))
    }

    return new Authentication(credentials.getUsername(), credentials.getPassword().getPlainText())
  }
}
