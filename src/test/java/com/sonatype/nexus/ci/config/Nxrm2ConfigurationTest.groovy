/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.config

import com.sonatype.nexus.api.ApiStub.NexusClientFactory
import com.sonatype.nexus.api.ApiStub.NxrmClient

import groovy.mock.interceptor.MockFor
import hudson.util.FormValidation
import hudson.util.FormValidation.Kind
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

class Nxrm2ConfigurationTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  def 'it validates the server url'() {
    setup:
      def configuration = (Nxrm2Configuration.DescriptorImpl) jenkins.getInstance().
          getDescriptor(Nxrm2Configuration.class)

    when:
      "validating $url"
      def validation = configuration.doCheckServerUrl(url)

    then:
      "it returns $result"
      validation.kind == result

    where:
      url              | result
      'foo'            | Kind.ERROR
      'http://foo.com' | Kind.OK
  }

  def 'it tests valid server credentials'() {
    when:
      GroovyMock(NexusClientFactory.class, global: true)
      def client = new MockFor(NxrmClient)
      client.demand.getNxrmRepositories { repositories }
      NexusClientFactory.buildRmClient(serverUrl, credentialsId) >> new NxrmClient()
      def configuration = (Nxrm2Configuration.DescriptorImpl) jenkins.getInstance().
          getDescriptor(Nxrm2Configuration.class)

    and:
      FormValidation validation = null
      client.use {
        validation = configuration.doVerifyCredentials(serverUrl, credentialsId)
      }

    then:
      validation.kind == Kind.OK
      validation.message == "Nexus Repository Manager 2.x connection succeeded (2 repositories)"

    where:
      serverUrl << ['serverUrl']
      credentialsId << ['credentialsId']
      repositories << [
          [
              [
                  id: 'maven-releases'
              ],
              [
                  id: 'nuget-releases'
              ]
          ]
      ]
  }

  def 'it tests invalid server credentials'() {
    when:
      GroovyMock(NexusClientFactory.class, global: true)
      def client = new MockFor(NxrmClient.class)
      client.demand.getNxrmRepositories {
        throw new IOException()
      }
      NexusClientFactory.buildRmClient(serverUrl, credentialsId) >> new NxrmClient()
      def configuration = (Nxrm2Configuration.DescriptorImpl) jenkins.getInstance().
          getDescriptor(Nxrm2Configuration.class)

    and:
      FormValidation validation = null
      client.use {
        validation = configuration.doVerifyCredentials(serverUrl, credentialsId)
      }

    then:
      validation.kind == Kind.ERROR
      validation.message.startsWith("Nexus Repository Manager 2.x connection failed")

    where:
      serverUrl << ['serverUrl']
      credentialsId << ['credentialsId']
  }
}
