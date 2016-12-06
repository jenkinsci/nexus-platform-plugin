/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.util

import com.sonatype.nexus.api.iq.IqClient

import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import hudson.util.Secret
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

class IqServerClientUtilTest
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
      IqServerClientUtil.buildIqClient(url, credentialsId)
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
      IqServerClientUtil.buildIqClient(url, credentialsId)
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
      IqClient client = IqServerClientUtil.buildIqClient(url, credentialsId)
    then:
      client != null
  }
}
