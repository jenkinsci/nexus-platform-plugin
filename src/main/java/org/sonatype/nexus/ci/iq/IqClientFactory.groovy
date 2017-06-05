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
import hudson.security.ACL
import jenkins.model.Jenkins
import org.slf4j.Logger

import static com.google.common.base.Preconditions.checkArgument
import static com.google.common.base.Preconditions.checkNotNull

class IqClientFactory
{
  static InternalIqClient getIqClient(Map conf, String credentialsId) {
    def serverUrl = conf.serverUrl ? URI.create(conf.serverUrl.toString()) : NxiqConfiguration.serverUrl
    def context = conf.context ?: Jenkins.instance
    def credentials = findCredentials(serverUrl, credentialsId, context)
    def serverConfig = getServerConfig(serverUrl, credentials)
    def proxyConfig = getProxyConfig(serverUrl)
    return (InternalIqClient) InternalIqClientBuilder.create()
        .withServerConfig(serverConfig)
        .withProxyConfig(proxyConfig)
        .withLogger(conf.log as Logger)
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
    checkNotNull(credentialsId)
    checkNotNull(context)
    checkArgument(!credentialsId.isEmpty())

    //noinspection GroovyAssignabilityCheck
    List<StandardCredentials> lookupCredentials = CredentialsProvider.lookupCredentials(
        StandardCredentials,
        context,
        ACL.SYSTEM,
        URIRequirementBuilder.fromUri(url.toString()).build())

    def credentials = CredentialsMatchers.firstOrNull(lookupCredentials, CredentialsMatchers.withId(credentialsId))
    Optional.ofNullable(credentials)
        .orElseThrow({ new IllegalArgumentException(Messages.IqClientFactory_NoCredentials(credentialsId)) })
  }

  static private ServerConfig getServerConfig(final URI url, final credentials) {
    if (credentials in StandardUsernamePasswordCredentials) {
      return new ServerConfig(url, new Authentication(credentials.getUsername(),
          credentials.getPassword().getPlainText()))
    } else if (credentials in StandardCertificateCredentials) {
      return new ServerConfig(url, new PkiAuthentication(credentials.getKeyStore(),
          credentials.password.plainText.toCharArray()))
    } else {
      throw new IllegalArgumentException(Messages.IqClientFactory_UnsupportedCredentials(credentials.class))
    }
  }
}
