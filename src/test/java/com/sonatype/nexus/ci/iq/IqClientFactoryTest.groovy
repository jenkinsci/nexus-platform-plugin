/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import com.sonatype.nexus.api.iq.IqClient
import com.sonatype.nexus.api.iq.internal.InternalIqClientBuilder
import com.sonatype.nexus.ci.config.NxiqConfiguration

import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
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
    and:
      1 * CredentialsMatchers.withId("123-cred-456")
  }

  def 'it uses job specific credentialsId when provided'() {
    setup:
      GroovyMock(NxiqConfiguration, global: true)
      GroovyMock(InternalIqClientBuilder, global: true)
      def iqClientBuilder = Mock(InternalIqClientBuilder)
      InternalIqClientBuilder.create() >> iqClientBuilder
      NxiqConfiguration.serverUrl >> URI.create("https://server/url/")
      NxiqConfiguration.credentialsId >> "123-cred-456"
      CredentialsMatchers.firstOrNull(_, _) >> credentials
      Logger logger = Mock()
    when:
      IqClientFactory.getIqClient(logger, 'job-specific-creds')
    then:
      1 * iqClientBuilder.withLogger(logger) >> iqClientBuilder
      1 * iqClientBuilder.withServerConfig { it.address == URI.create("https://server/url/") } >> iqClientBuilder
    and:
      1 * CredentialsMatchers.withId("job-specific-creds")
  }
}
