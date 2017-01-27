/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import com.sonatype.nexus.api.common.ServerConfig
import com.sonatype.nexus.api.iq.IqClient
import com.sonatype.nexus.api.iq.IqClientBuilder
import com.sonatype.nexus.api.iq.internal.InternalIqClientBuilder
import com.sonatype.nexus.ci.config.GlobalNexusConfiguration
import com.sonatype.nexus.ci.config.NxiqConfiguration

import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import hudson.ProxyConfiguration
import hudson.util.Secret
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import org.slf4j.Logger
import spock.lang.Specification

class IqClientFactoryTest
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
      IqClientFactory.getIqClient(URI.create(url), credentialsId)
    then:
      IllegalArgumentException ex = thrown()
      ex.message =~ /No credentials were found for credentialsId: 42/
  }

  def 'it creates a client when credentialsId given'() {
    setup:
      def url = 'http://foo.com'
      def credentialsId = "42"
      CredentialsMatchers.firstOrNull(_, _) >> credentials
    when:
      IqClient client = IqClientFactory.getIqClient(URI.create(url), credentialsId)
    then:
      client != null
  }

  def 'it uses configured serverUrl and credentialsId'() {
    setup:
      GroovyMock(NxiqConfiguration, global: true)
      GroovyMock(InternalIqClientBuilder, global: true)
      def iqClientBuilder = Mock(InternalIqClientBuilder)
      InternalIqClientBuilder.create() >> iqClientBuilder
      NxiqConfiguration.serverUrl >> URI.create("https://server/url/")
      NxiqConfiguration.credentialsId >> "123-cred-456"
      CredentialsMatchers.firstOrNull(_, _) >> credentials
    when:
      IqClientFactory.getIqClient()
    then:
      1 * iqClientBuilder.withServerConfig { it.address == URI.create("https://server/url/") } >> iqClientBuilder
      1 * iqClientBuilder.withProxyConfig(_) >> iqClientBuilder
    and:
      1 * CredentialsMatchers.withId("123-cred-456")
  }

  def 'it uses job specific credentialsId when provided'() {
    setup:
      final String serverUrl = 'http://localhost/'
      final String credentialsId = '123-cred-456'

      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration(serverUrl, credentialsId)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

      jenkinsRule.instance.proxy = new ProxyConfiguration('http://proxy/url', 9080, null, null, '')

      CredentialsMatchers.firstOrNull(_, _) >> credentials

      GroovyMock(IqClientBuilder, global: true)
      def iqClientBuilder = Mock(InternalIqClientBuilder)
      InternalIqClientBuilder.create() >> iqClientBuilder

    when:
      IqClientFactory.getIqClient((Logger)null, 'job-specific-creds')
    then:
      1 * CredentialsMatchers.withId('job-specific-creds')
  }

  def 'it uses configured proxy when configured'() {
    setup:
      final String serverUrl = 'http://localhost/'
      final String credentialsId = '123-cred-456'

      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration(serverUrl, credentialsId)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

      jenkinsRule.instance.proxy = new ProxyConfiguration('http://proxy/url', 9080, null, null, '')

      CredentialsMatchers.firstOrNull(_, _) >> credentials

      GroovyMock(InternalIqClientBuilder, global: true)
      def iqClientBuilder = Mock(InternalIqClientBuilder)
      InternalIqClientBuilder.create() >> iqClientBuilder
      iqClientBuilder.withLogger(_) >> iqClientBuilder

    when:
      clientGetter()
    then:
      1 * iqClientBuilder.withServerConfig { ServerConfig config ->
        config.address == URI.create(expectedServerUrl)
      } >> iqClientBuilder
      1 * iqClientBuilder.withProxyConfig { ServerConfig config ->
        config.address == URI.create('http://proxy:9080/url/')
      } >> iqClientBuilder

    where:
      clientGetter << [
          { -> IqClientFactory.getIqClient() },
          { -> IqClientFactory.getIqClient(URI.create('http://127.0.0.1/'), '') },
          { -> IqClientFactory.getIqClient((Logger) null, '') },
          { -> IqClientFactory.getIqClient(URI.create('http://127.0.0.1/'), (Logger) null) },
      ]
      expectedServerUrl << [
        'http://localhost/',
        'http://127.0.0.1/',
        'http://localhost/',
        'http://127.0.0.1/'
      ]
  }
}
