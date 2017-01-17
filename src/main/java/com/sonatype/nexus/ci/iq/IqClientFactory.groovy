/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import javax.annotation.Nullable

import com.sonatype.nexus.api.common.Authentication
import com.sonatype.nexus.api.common.ServerConfig
import com.sonatype.nexus.api.iq.IqClient
import com.sonatype.nexus.api.iq.IqClientBuilder
import com.sonatype.nexus.ci.config.NxiqConfiguration

import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder
import hudson.model.ItemGroup
import hudson.security.ACL

class IqClientFactory
{
  static IqClient getIqClient() {
    return buildIqClient(new URI(NxiqConfiguration.serverUrl), NxiqConfiguration.credentialsId)
  }

  static IqClient buildIqClient(URI url, @Nullable String credentialsId) {
    def serverConfig
    if (credentialsId) {
      def authentication = loadCredentials(url, credentialsId)
      serverConfig = new ServerConfig(url, authentication)
    }
    else {
      serverConfig = new ServerConfig(url)
    }

    //TODO probably need to add proxy support
    return IqClientBuilder.create().withServerConfig(serverConfig).build()
  }

  static private Authentication loadCredentials(final URI url, final String credentialsId) {
    def lookupCredentials = CredentialsProvider.lookupCredentials(
        StandardUsernamePasswordCredentials.class,
        (ItemGroup) jenkins.model.Jenkins.getInstance(),
        ACL.SYSTEM,
        URIRequirementBuilder.fromUri(url.toString()).build())

    def credentials = CredentialsMatchers.firstOrNull(lookupCredentials, CredentialsMatchers.withId(credentialsId))
    if (!credentials) {
      throw new IllegalArgumentException("No credentials were found for credentialsId: ${credentialsId}")
    }

    return new Authentication(credentials.getUsername(), credentials.getPassword().getPlainText())
  }
}
