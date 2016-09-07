/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.util

import com.sonatype.nexus.api.common.Authentication
import com.sonatype.nexus.api.common.ServerConfig
import com.sonatype.nexus.api.repository.RepositoryManagerClient
import com.sonatype.nexus.api.repository.RepositoryManagerClientBuilder

import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder
import hudson.security.ACL

class RepositoryManagerClientUtil
{
  static RepositoryManagerClient buildRmClient(String url, String credentialsId) throws URISyntaxException {
    def uri = new URI(url + "/service/local/artifact/maven/content");

    def credentials = CredentialsMatchers.firstOrNull(CredentialsProvider
        .lookupCredentials(StandardUsernamePasswordCredentials.class, jenkins.model.Jenkins.getInstance(), ACL.SYSTEM,
        URIRequirementBuilder.fromUri(url).build()), CredentialsMatchers.withId(credentialsId));

    def authentication

    if (credentialsId) {
      if (!credentials) {
        throw new IllegalArgumentException("No credentials were found for credentialsId: ${credentialsId}")
      }
      authentication = new Authentication(credentials.getUsername(), credentials.getPassword().getPlainText())
    }

    def serverConfig = authentication != null ? new ServerConfig(uri, authentication) : new ServerConfig(uri)

    //TODO probably need to add proxy support
    return new RepositoryManagerClientBuilder().create().withServerConfig(serverConfig).build();
  }
}
