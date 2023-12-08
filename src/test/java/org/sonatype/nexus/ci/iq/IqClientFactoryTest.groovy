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

import java.security.KeyStore

import com.sonatype.nexus.api.common.ProxyConfig
import com.sonatype.nexus.api.common.ServerConfig
import com.sonatype.nexus.api.iq.IqClient
import com.sonatype.nexus.api.iq.IqClientBuilder
import com.sonatype.nexus.api.iq.internal.InternalIqClientBuilder

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.NxiqConfiguration

import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials
import com.cloudbees.plugins.credentials.common.StandardCredentials
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import hudson.ProxyConfiguration
import hudson.model.Job
import hudson.util.Secret
import org.jenkinsci.plugins.plaincredentials.StringCredentials
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import org.slf4j.Logger
import spock.lang.Specification

import static org.hamcrest.CoreMatchers.allOf
import static org.hamcrest.CoreMatchers.endsWith
import static org.hamcrest.CoreMatchers.startsWith

class IqClientFactoryTest
  extends Specification
{
  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule()

  StandardCredentials credentials = Mock(StandardUsernamePasswordCredentials)
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
      IqClientFactory.getIqClient(
          new IqClientFactoryConfiguration(credentialsId: credentialsId, serverUrl: URI.create(url)))
    then:
      IllegalArgumentException ex = thrown()
      ex.message =~ /No credentials were found for credentials 42/
  }

  def 'it creates a client when credentialsId given'() {
    setup:
      def url = 'http://foo.com'
      def credentialsId = "42"
      CredentialsMatchers.firstOrNull(_, _) >> credentials
    when:
      IqClient client = IqClientFactory.getIqClient(
          new IqClientFactoryConfiguration(credentialsId: credentialsId, serverUrl: URI.create(url)))
    then:
      client != null
  }

  def 'it creates a client with certificate credentials'() {
    setup:
      def url = 'http://foo.com'
      def credentialsId = "42"
      def cert = Mock(StandardCertificateCredentials)
      CredentialsMatchers.firstOrNull(_, _) >> cert

    when:
      def client = IqClientFactory.getIqClient(
          new IqClientFactoryConfiguration(credentialsId: credentialsId, serverUrl: URI.create(url)))

    then:
      1 * cert.getKeyStore() >> Mock(KeyStore)
      1 * cert.getPassword() >> secret
      client != null
  }

  def 'it throws exception for unsupported credential types'() {
    setup:
      def url = 'http://foo.com'
      def credentialsId = "42"
      def unsupportedCreds = Mock(StringCredentials)
      CredentialsMatchers.firstOrNull(_, _) >> unsupportedCreds

    when:
      IqClientFactory.getIqClient(
          new IqClientFactoryConfiguration(credentialsId: credentialsId, serverUrl: URI.create(url)))

    then:
      Throwable exception = thrown()
      exception.message == 'Credentials of type ' + unsupportedCreds.class.simpleName + ' are not supported'
  }

  def 'it uses configured serverUrl and credentialsId'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id', 'internalId', 'displayName', 'https://server/url/',
          '123-cred-456', false))
      globalConfiguration.save()
      GroovyMock(InternalIqClientBuilder, global: true)
      def iqClientBuilder = Mock(InternalIqClientBuilder)
      InternalIqClientBuilder.create() >> iqClientBuilder
      CredentialsMatchers.firstOrNull(_, _) >> credentials

    when:
      IqClientFactory.getIqClient()

    then:
      1 * iqClientBuilder.withServerConfig { it.address == URI.create("https://server/url/") } >> iqClientBuilder
      1 * iqClientBuilder.withProxyConfig(_) >> iqClientBuilder
      1 * iqClientBuilder.withUserAgent(_) >> iqClientBuilder
      1 * iqClientBuilder.withLogger(_) >> iqClientBuilder

    and:
      1 * CredentialsMatchers.withId("123-cred-456")
  }

  def 'it uses job specific credentials when provided'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration('id', 'internalId', 'displayName', 'http://localhost/',
          'credentialsId', false)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

      CredentialsMatchers.firstOrNull(_, _) >> credentials

      GroovyMock(InternalIqClientBuilder, global: true)
      def iqClientBuilder = Mock(InternalIqClientBuilder)
      InternalIqClientBuilder.create() >> iqClientBuilder

    when:
      IqClientFactory.getIqClient(
          new IqClientFactoryConfiguration(credentialsId: 'jobSpecificCredentialsId', context: Mock(Job)))
    then:
      1 * iqClientBuilder.withServerConfig { ServerConfig config ->
        config.address == URI.create('http://localhost/')
      } >> iqClientBuilder
      1 * iqClientBuilder.withProxyConfig(_) >> iqClientBuilder
      1 * iqClientBuilder.withUserAgent(_) >> iqClientBuilder
      1 * iqClientBuilder.withLogger(_) >> iqClientBuilder
      1 * CredentialsMatchers.withId('jobSpecificCredentialsId')
  }

  def 'it interprets an empty String job credentials as not provided'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration('id', 'internalId', 'displayName', 'http://localhost/',
          'credentialsId', false)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

      CredentialsMatchers.firstOrNull(_, _) >> credentials

      GroovyMock(InternalIqClientBuilder, global: true)
      def iqClientBuilder = Mock(InternalIqClientBuilder)
      InternalIqClientBuilder.create() >> iqClientBuilder

    when:
      IqClientFactory.getIqClient(new IqClientFactoryConfiguration(credentialsId: ''))

    then:
      1 * iqClientBuilder.withServerConfig { ServerConfig config ->
        config.address == URI.create('http://localhost/')
      } >> iqClientBuilder
      1 * iqClientBuilder.withProxyConfig(_) >> iqClientBuilder
      1 * iqClientBuilder.withUserAgent(_) >> iqClientBuilder
      1 * iqClientBuilder.withLogger(_) >> iqClientBuilder
      1 * CredentialsMatchers.withId('credentialsId')
  }

  def 'it uses job specific credentialsId when provided'() {
    setup:
      final String serverUrl = 'http://localhost/'
      final String credentialsId = '123-cred-456'

      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration('id', 'internalId', 'displayName', serverUrl, credentialsId, false)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

      jenkinsRule.instance.proxy = new ProxyConfiguration('http://proxy/url', 9080, null, null, '')

      CredentialsMatchers.firstOrNull(_, _) >> credentials

      GroovyMock(IqClientBuilder, global: true)
      def iqClientBuilder = Mock(InternalIqClientBuilder)
      InternalIqClientBuilder.create() >> iqClientBuilder

    when:
      IqClientFactory.getIqClient(new IqClientFactoryConfiguration(credentialsId: 'job-specific-creds'))

    then:
      1 * CredentialsMatchers.withId('job-specific-creds')
  }

  def 'it uses configured proxy when configured'() {
    setup:
      final String serverUrl = 'http://localhost/'
      final String credentialsId = '123-cred-456'

      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration('id', 'internalId', 'displayName', serverUrl, credentialsId, false)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

      jenkinsRule.instance.proxy = new ProxyConfiguration('localhost', 8888, null, null, '')

      CredentialsMatchers.firstOrNull(_, _) >> credentials

      GroovyMock(InternalIqClientBuilder, global: true)
      def iqClientBuilder = Mock(InternalIqClientBuilder)
      InternalIqClientBuilder.create() >> iqClientBuilder
      iqClientBuilder.withLogger(_) >> iqClientBuilder
      iqClientBuilder.withUserAgent(_) >> iqClientBuilder

    when:
      clientGetter()
    then:
      1 * iqClientBuilder.withServerConfig { ServerConfig config ->
        config.address == URI.create(expectedServerUrl)
      } >> iqClientBuilder
      1 * iqClientBuilder.withProxyConfig { ProxyConfig proxy ->
        proxy.host == 'localhost'
        proxy.port == 8888
        proxy.authentication == null
      } >> iqClientBuilder

    where:
      clientGetter << [
          { -> IqClientFactory.getIqClient(new IqClientFactoryConfiguration()) },
          { -> IqClientFactory.getIqClient(
              new IqClientFactoryConfiguration(credentialsId: '123-cred-456', serverUrl: URI.create('http://127.0.0.1/'))) },
          { -> IqClientFactory.getIqClient(new IqClientFactoryConfiguration(credentialsId: '123-cred-456')) }
      ]
      expectedServerUrl << [
        'http://localhost/',
        'http://127.0.0.1/',
        'http://localhost/'
      ]
  }

  def 'it uses configured proxy with authentication when configured'() {
    setup:
      final String serverUrl = 'http://localhost/'
      final String credentialsId = '123-cred-456'

      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration('id', 'internalId', 'displayName', serverUrl, credentialsId, false)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

      jenkinsRule.instance.proxy = new ProxyConfiguration('localhost', 8888, 'username', 'password', '')

      CredentialsMatchers.firstOrNull(_, _) >> credentials

      GroovyMock(InternalIqClientBuilder, global: true)
      def iqClientBuilder = Mock(InternalIqClientBuilder)
      InternalIqClientBuilder.create() >> iqClientBuilder
      iqClientBuilder.withUserAgent(_) >> iqClientBuilder
      iqClientBuilder.withLogger(_) >> iqClientBuilder
      iqClientBuilder.withInstanceId(_) >> iqClientBuilder

    when:
      IqClientFactory.getIqClient()
    then:
      1 * iqClientBuilder.withServerConfig { ServerConfig config ->
        config.address == URI.create(serverUrl)
      } >> iqClientBuilder
      1 * iqClientBuilder.withProxyConfig { ProxyConfig proxy ->
        proxy.host == 'localhost'
        proxy.port == 8888
        proxy.authentication.username == 'username'
        proxy.authentication.password.toString() == 'password'
      } >> iqClientBuilder
  }

  def 'getLocalIqClient uses logger and instanceId but does not populate the server or proxy configuration'() {
    GroovyMock(InternalIqClientBuilder, global: true)
    def iqClientBuilder = Mock(InternalIqClientBuilder)
    InternalIqClientBuilder.create() >> iqClientBuilder

    def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
    globalConfiguration.iqConfigs = []
    globalConfiguration.iqConfigs.add(new NxiqConfiguration('id', 'internalId', 'displayName', 'http://localhost/',
        'credentialsId', false))
    globalConfiguration.save()

    def logger = Mock(Logger)

    when:
      IqClientFactory.getIqLocalClient(logger, 'instanceId')

    then:
      1 * iqClientBuilder.withUserAgent(_) >> iqClientBuilder
      1 * iqClientBuilder.withLogger(logger) >> iqClientBuilder
      1 * iqClientBuilder.withInstanceId('instanceId') >> iqClientBuilder
      0 * iqClientBuilder.withServerConfig(_)
      0 * iqClientBuilder.withProxyConfig(_)
  }

  def 'getIqClient uses custom user agent string'() {
    setup:
      def url = 'https://foo.com'
      def credentialsId = "42"
      CredentialsMatchers.firstOrNull(_, _) >> credentials

      GroovyMock(InternalIqClientBuilder, global: true)
      def iqClientBuilder = Mock(InternalIqClientBuilder)
      InternalIqClientBuilder.create() >> iqClientBuilder

      def jenkinsVersion = System.getProperty('jenkins.version')?:'2.361.1'

    when:
      IqClientFactory.getIqClient(
          new IqClientFactoryConfiguration(credentialsId: credentialsId, serverUrl: URI.create(url)))

    then:
      1 * iqClientBuilder.withServerConfig(_) >> iqClientBuilder
      1 * iqClientBuilder.withProxyConfig(_) >> iqClientBuilder
      1 * iqClientBuilder.withUserAgent(
          allOf(startsWith('Sonatype_CLM_CI_Jenkins/'), endsWith("Jenkins ${jenkinsVersion})"))) >> iqClientBuilder
      1 * iqClientBuilder.withLogger(_) >> iqClientBuilder
  }
}
