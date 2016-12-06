/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.util

import com.sonatype.nexus.api.common.Authentication
import com.sonatype.nexus.api.common.ServerConfig
import com.sonatype.nexus.api.iq.IqClient
import com.sonatype.nexus.api.iq.IqClientBuilder

import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder
import hudson.security.ACL

class IqServerClientUtil
{
  static IqClient buildIqClient(String url, String credentialsId) throws URISyntaxException {
    def uri = new URI(url)

    def lookupCredentials = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,
        jenkins.model.Jenkins.getInstance(),
        ACL.SYSTEM,
        URIRequirementBuilder.fromUri(url).build())

    def credentials = CredentialsMatchers.firstOrNull(lookupCredentials, CredentialsMatchers.withId(credentialsId))

    def authentication

    if (credentialsId) {
      if (!credentials) {
        throw new IllegalArgumentException("No credentials were found for credentialsId: ${credentialsId}")
      }
      authentication = new Authentication(credentials.getUsername(), credentials.getPassword().getPlainText())
    }

    def serverConfig = authentication != null ? new ServerConfig(uri, authentication) : new ServerConfig(uri)

    //TODO probably need to add proxy support
    return new IqClientBuilder().create().withServerConfig(serverConfig).build()
  }
}
