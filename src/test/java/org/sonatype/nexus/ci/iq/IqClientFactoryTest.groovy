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

import com.sonatype.nexus.api.common.ProxyConfig
import com.sonatype.nexus.api.common.ServerConfig
import com.sonatype.nexus.api.iq.IqClient
import com.sonatype.nexus.api.iq.IqClientBuilder
import com.sonatype.nexus.api.iq.internal.InternalIqClientBuilder

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.NxiqConfiguration

import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import hudson.ProxyConfiguration
import hudson.model.Project
import hudson.util.Secret
import jenkins.model.Jenkins
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
      IqClientFactory.getIqClient(credentialsId, serverUrl: url, context: Jenkins.instance)
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
      IqClient client = IqClientFactory.getIqClient(credentialsId, serverUrl: URI.create(url), context: Jenkins.instance)
    then:
      client != null
  }

  def 'it uses job specific credentials when provided'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration('http://localhost/', 'credentialsId')
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

      CredentialsMatchers.firstOrNull(_, _) >> credentials

      GroovyMock(InternalIqClientBuilder, global: true)
      def iqClientBuilder = Mock(InternalIqClientBuilder)
      InternalIqClientBuilder.create() >> iqClientBuilder

    when:
      IqClientFactory.getIqClient('jobSpecificCredentialsId', context: Mock(Project))
    then:
      1 * iqClientBuilder.withServerConfig { ServerConfig config ->
        config.address == URI.create('http://localhost/')
      } >> iqClientBuilder
      1 * iqClientBuilder.withProxyConfig(_) >> iqClientBuilder
      iqClientBuilder.withLogger(_) >> iqClientBuilder
      1 * CredentialsMatchers.withId('jobSpecificCredentialsId')
  }

  def 'it interprets an empty String job credentials as not provided'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration('http://localhost/', 'credentialsId')
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

      CredentialsMatchers.firstOrNull(_, _) >> credentials

      GroovyMock(InternalIqClientBuilder, global: true)
      def iqClientBuilder = Mock(InternalIqClientBuilder)
      InternalIqClientBuilder.create() >> iqClientBuilder

    when:
      IqClientFactory.getIqClient('', context: Mock(Project))

    then:
      thrown(IllegalArgumentException)
  }

  def 'it uses logger and job specific credentialsId when provided'() {
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
      IqClientFactory.getIqClient('job-specific-creds', context: Jenkins.instance)
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

      jenkinsRule.instance.proxy = new ProxyConfiguration('localhost', 8888, null, null, '')

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
      1 * iqClientBuilder.withProxyConfig { ProxyConfig proxy ->
        proxy.host == 'localhost'
        proxy.port == 8888
        proxy.authentication == null
      } >> iqClientBuilder

    where:
      clientGetter << [
          { -> IqClientFactory.getIqClient('123-cred-456', serverUrl: URI.create('http://127.0.0.1/'), context: Jenkins.instance) },
          { -> IqClientFactory.getIqClient('123-cred-456', context: Jenkins.instance) }
      ]
      expectedServerUrl << [
        'http://127.0.0.1/',
        'http://localhost/'
      ]
  }

  def 'it uses configured proxy with authentication when configured'() {
    setup:
      final String serverUrl = 'http://localhost/'
      final String credentialsId = '123-cred-456'

      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration(serverUrl, credentialsId)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

      jenkinsRule.instance.proxy = new ProxyConfiguration('localhost', 8888, 'username', 'password', '')

      CredentialsMatchers.firstOrNull(_, _) >> credentials

      GroovyMock(InternalIqClientBuilder, global: true)
      def iqClientBuilder = Mock(InternalIqClientBuilder)
      InternalIqClientBuilder.create() >> iqClientBuilder
      iqClientBuilder.withLogger(_) >> iqClientBuilder
      iqClientBuilder.withInstanceId(_) >> iqClientBuilder

    when:
      IqClientFactory.getIqClient(credentialsId, serverUrl: serverUrl, context: jenkinsRule.instance)
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
    globalConfiguration.iqConfigs.add(new NxiqConfiguration('http://localhost/', 'credentialsId'))
    globalConfiguration.save()

    def logger = Mock(Logger)

    when:
      IqClientFactory.getIqLocalClient(logger, 'instanceId')

    then:
      1 * iqClientBuilder.withLogger(logger) >> iqClientBuilder
      1 * iqClientBuilder.withInstanceId('instanceId') >> iqClientBuilder
      0 * iqClientBuilder.withServerConfig(_)
      0 * iqClientBuilder.withProxyConfig(_)
  }
}
