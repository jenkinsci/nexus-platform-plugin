/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.util

import com.sonatype.nexus.api.common.ProxyConfig
import com.sonatype.nexus.api.repository.RepositoryManagerClient
import com.sonatype.nexus.api.repository.RepositoryManagerClientBuilder

import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import hudson.ProxyConfiguration
import hudson.util.Secret
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

class RepositoryManagerClientUtilTest
  extends Specification
{
  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule()

  StandardUsernamePasswordCredentials credentials = Mock()
  Secret secret = new Secret("password")

  def setup() {
    credentials.getUsername() >> "username"
    credentials.getPassword() >> secret

    GroovyMock(CredentialsMatchers.class, global: true)
  }

  def 'it throws exception when credentialsId given but no credentials found'() {
    setup:
      def url = 'http://foo.com'
      def credentialsId = '42'
      CredentialsMatchers.firstOrNull(_, _) >> null
    when:
      RepositoryManagerClientUtil.buildRmClient(url, credentialsId)
    then:
      IllegalArgumentException ex = thrown()
      ex.message =~ /No credentials were found for credentialsId: 42/
  }

  def 'it creates an anonymous client when no credentialsId given'() {
    setup:
      def url = 'http://foo.com'
      def credentialsId = null
      CredentialsMatchers.firstOrNull(_, _) >> null
    when:
      RepositoryManagerClient client = RepositoryManagerClientUtil.buildRmClient(url, credentialsId)
    then:
      client != null
  }

  def 'it creates a client when credentialsId given'() {
    setup:
      def url = 'http://foo.com'
      def credentialsId = "42"
      CredentialsMatchers.firstOrNull(_, _) >> credentials
    when:
      RepositoryManagerClient client = RepositoryManagerClientUtil.buildRmClient(url, credentialsId)
    then:
      client != null
  }

  def 'it creates a client with proxy when configured'() {
    setup:
      GroovyMock(RepositoryManagerClientBuilder, global: true)
      def clientBuilder = Mock(RepositoryManagerClientBuilder)
      RepositoryManagerClientBuilder.create() >> clientBuilder
      def url = 'http://foo.com'
      def credentialsId = "42"
      CredentialsMatchers.firstOrNull(_, _) >> credentials

      jenkinsRule.instance.proxy = new ProxyConfiguration('localhost', 8888, null, null, '')

    when:
      RepositoryManagerClientUtil.buildRmClient(url, credentialsId)
    then:
      1 * clientBuilder.withServerConfig{ it.address == URI.create("http://foo.com/") } >> clientBuilder
      1 * clientBuilder.withProxyConfig { ProxyConfig config ->
        config.host == 'localhost'
        config.port == 8888
        config.authentication == null
      } >> clientBuilder
  }

  def 'it creates a client with proxy with authentication when configured'() {
    setup:
      GroovyMock(RepositoryManagerClientBuilder, global: true)
      def clientBuilder = Mock(RepositoryManagerClientBuilder)
      RepositoryManagerClientBuilder.create() >> clientBuilder
      def url = 'http://foo.com'
      def credentialsId = "42"
      CredentialsMatchers.firstOrNull(_, _) >> credentials

      jenkinsRule.instance.proxy = new ProxyConfiguration('localhost', 8888, 'username', 'password', '')

    when:
      RepositoryManagerClientUtil.buildRmClient(url, credentialsId)
    then:
      1 * clientBuilder.withServerConfig{ it.address == URI.create("http://foo.com/") } >> clientBuilder
      1 * clientBuilder.withProxyConfig { ProxyConfig config ->
        config.host == 'localhost'
        config.port == 8888
        config.authentication.username == 'username'
        config.authentication.password.toString() == 'password'
      } >> clientBuilder
  }
}
