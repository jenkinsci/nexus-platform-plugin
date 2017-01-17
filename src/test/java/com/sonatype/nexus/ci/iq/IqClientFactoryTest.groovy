/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import com.sonatype.nexus.api.iq.IqClient
import com.sonatype.nexus.api.iq.IqClientBuilder
import com.sonatype.nexus.ci.config.NxiqConfiguration
import com.sonatype.nexus.ci.iq.IqClientFactory
import com.sonatype.nexus.ci.util.IqUtil

import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import hudson.util.Secret
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
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
      IqClientFactory.buildIqClient(URI.create(url), credentialsId)
    then:
      IllegalArgumentException ex = thrown()
      ex.message =~ /No credentials were found for credentialsId: 42/
  }

  def 'it requires authentication'() {
    setup:
      def url = 'http://foo.com'
      def credentialsId = null
      CredentialsMatchers.firstOrNull(_, _) >> null
    when:
      IqClientFactory.buildIqClient(URI.create(url), credentialsId)
    then:
      NullPointerException ex = thrown()
      ex.message =~ /IQ Server authentication is required/
  }

  def 'it creates a client when credentialsId given'() {
    setup:
      def url = 'http://foo.com'
      def credentialsId = "42"
      CredentialsMatchers.firstOrNull(_, _) >> credentials
    when:
      IqClient client = IqClientFactory.buildIqClient(URI.create(url), credentialsId)
    then:
      client != null
  }

  def 'it uses configured serverUrl and credentialsId'() {
    setup:
      GroovyMock(NxiqConfiguration, global: true)
      GroovyMock(IqClientBuilder, global: true)
      def iqClientBuilder = Mock(IqClientBuilder)
      IqClientBuilder.create() >> iqClientBuilder
      NxiqConfiguration.serverUrl >> "https://server/url/"
      NxiqConfiguration.credentialsId >> "123-cred-456"
      CredentialsMatchers.firstOrNull(_, _) >> credentials
    when:
      IqClientFactory.getIqClient()
    then:
      1 * iqClientBuilder.withServerConfig { it.address == URI.create("https://server/url/") } >> iqClientBuilder
    and:
      1 * CredentialsMatchers.withId("123-cred-456")
  }
}
