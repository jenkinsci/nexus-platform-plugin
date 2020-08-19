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

import com.sonatype.nexus.api.common.ProxyConfig
import com.sonatype.nexus.api.repository.v2.RepositoryManagerV2Client
import com.sonatype.nexus.api.repository.v2.RepositoryManagerV2ClientBuilder
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3ClientBuilder

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
      RepositoryManagerClientUtil.nexus2Client(url, credentialsId)
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
      RepositoryManagerV2Client client = RepositoryManagerClientUtil.nexus2Client(url, credentialsId)
    then:
      client != null
  }

  def 'it creates a client when credentialsId given'() {
    setup:
      def url = 'http://foo.com'
      def credentialsId = "42"
      CredentialsMatchers.firstOrNull(_, _) >> credentials
    when:
      RepositoryManagerV2Client client = RepositoryManagerClientUtil.nexus2Client(url, credentialsId)
    then:
      client != null
  }

  def 'it creates a client with proxy when configured'() {
    setup:
      GroovyMock(RepositoryManagerV2ClientBuilder, global: true)
      def clientBuilder = Mock(RepositoryManagerV2ClientBuilder)
      RepositoryManagerV2ClientBuilder.create() >> clientBuilder
      def url = 'http://foo.com'
      def credentialsId = "42"
      CredentialsMatchers.firstOrNull(_, _) >> credentials

      jenkinsRule.instance.proxy = new ProxyConfiguration('localhost', 8888, null, null, '')

    when:
      RepositoryManagerClientUtil.nexus2Client(url, credentialsId)
    then:
      1 * clientBuilder.withServerConfig{ it.address == URI.create("http://foo.com/") } >> clientBuilder
      1 * clientBuilder.withProxyConfig { ProxyConfig config ->
        config.host == 'localhost'
        config.port == 8888
        config.authentication == null
      } >> clientBuilder
  }

  def 'it creates a v3 client with proxy when configured '() {
    setup:
      GroovyMock(RepositoryManagerV3ClientBuilder, global: true)
      def clientBuilder = Mock(RepositoryManagerV3ClientBuilder)
      RepositoryManagerV3ClientBuilder.create() >> clientBuilder
      def url = 'http://foo.com'
      def credentialsId = "42"
      CredentialsMatchers.firstOrNull(_, _) >> credentials

      jenkinsRule.instance.proxy = new ProxyConfiguration('localhost', 8888, null, null, '')

    when:
      RepositoryManagerClientUtil.nexus3Client(url, credentialsId)
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
      GroovyMock(RepositoryManagerV2ClientBuilder, global: true)
      def clientBuilder = Mock(RepositoryManagerV2ClientBuilder)
      RepositoryManagerV2ClientBuilder.create() >> clientBuilder
      def url = 'http://foo.com'
      def credentialsId = "42"
      CredentialsMatchers.firstOrNull(_, _) >> credentials

      jenkinsRule.instance.proxy = new ProxyConfiguration('localhost', 8888, 'username', 'password', '')

    when:
      RepositoryManagerClientUtil.nexus2Client(url, credentialsId)
    then:
      1 * clientBuilder.withServerConfig{ it.address == URI.create("http://foo.com/") } >> clientBuilder
      1 * clientBuilder.withProxyConfig { ProxyConfig config ->
        config.host == 'localhost'
        config.port == 8888
        config.authentication.username == 'username'
        config.authentication.password.toString() == 'password'
      } >> clientBuilder
  }

  def 'it creates a v3 client with proxy with authentication when configured'() {
    setup:
      GroovyMock(RepositoryManagerV3ClientBuilder, global: true)
      def clientBuilder = Mock(RepositoryManagerV3ClientBuilder)
      RepositoryManagerV3ClientBuilder.create() >> clientBuilder
      def url = 'http://foo.com'
      def credentialsId = "42"
      CredentialsMatchers.firstOrNull(_, _) >> credentials

      jenkinsRule.instance.proxy = new ProxyConfiguration('localhost', 8888, 'username', 'password', '')

    when:
      RepositoryManagerClientUtil.nexus3Client(url, credentialsId)
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
