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
package org.sonatype.nexus.ci.config

import com.sonatype.nexus.api.exception.IqClientException
import com.sonatype.nexus.api.iq.internal.InternalIqClient

import org.sonatype.nexus.ci.iq.IqClientFactory

import hudson.util.FormValidation
import hudson.util.FormValidation.Kind
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

class NxiqConfigurationTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  def 'it validates that display name is unique'() {
    setup:
      saveConfig('id', 'displayName')

    when:
      "validating $displayName"
      def validation = descriptor.doCheckDisplayName(displayName, 'otherId')

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml().startsWith(message)

    where:
      displayName           | kind       | message
      'displayName'         | Kind.ERROR | 'Display Name must be unique'
      'Other Display Name'  | Kind.OK    | '<div/>'
  }

  def 'it validates that display name is required'() {
    when:
      "validating $displayName"
      def validation = descriptor.doCheckDisplayName(displayName, 'id')

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml().startsWith(message)

    where:
      displayName              | kind       | message
      ''                       | Kind.ERROR | 'Display Name is required'
      null                     | Kind.ERROR | 'Display Name is required'
      'Other Display Name'     | Kind.OK    | '<div/>'
  }
  
  def 'it validates that id is unique'() {
    setup:
      saveConfig('id', 'foo')

    when:
      "validating $id"
      def validation = descriptor.doCheckId(id, 'otherId')

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml().startsWith(message)

    where:
      id          | kind       | message
      'id'        | Kind.ERROR | 'Server ID must be unique'
      'other_id'  | Kind.OK    | '<div/>'
  }

  def 'it validates that id is required'() {
    when:
      "validating $id"
      def validation = descriptor.doCheckId(id, 'id')

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml().startsWith(message)

    where:
      id           | kind       | message
      ''           | Kind.ERROR | 'Server ID is required'
      null         | Kind.ERROR | 'Server ID is required'
      'other_id'   | Kind.OK    | '<div/>'
  }

  def 'it validates that id is contains no whitespace'() {
    when:
      "validating $id"
      def validation = descriptor.doCheckId(id, 'id')

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml().startsWith(message)

    where:
      id            | kind       | message
      ' id'         | Kind.ERROR | 'Server ID must not contain whitespace'
      'i d'         | Kind.ERROR | 'Server ID must not contain whitespace'
      'id '         | Kind.ERROR | 'Server ID must not contain whitespace'
      'other_id'    | Kind.OK    | '<div/>'
  }

  def 'it validates the server url is a valid url'() {
    setup:
      def configuration = (NxiqConfiguration.DescriptorImpl) jenkins.getInstance().
          getDescriptor(NxiqConfiguration.class)

    when:
      "validating $url"
      def validation = configuration.doCheckServerUrl(url)

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml().startsWith(message)

    where:
      url              | kind       | message
      'foo'            | Kind.ERROR | 'Malformed url (no protocol: foo)'
      'http://foo.com' | Kind.OK    | '<div/>'
  }

  def 'it validates the server url is required'() {
    setup:
      def configuration = (NxiqConfiguration.DescriptorImpl) jenkins.getInstance().
          getDescriptor(NxiqConfiguration.class)

    when:
      "validating $url"
      def validation = configuration.doCheckServerUrl(url)

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml().startsWith(message)

    where:
      url              | kind       | message
      ''               | Kind.ERROR | 'Server URL is required'
      null             | Kind.ERROR | 'Server URL is required'
      'http://foo.com' | Kind.OK    | '<div/>'
  }

  def 'it tests valid server credentials'() {
    when:
      GroovyMock(IqClientFactory.class, global: true)
      def client = Mock(InternalIqClient.class)
      client.getApplicationsForApplicationEvaluation() >> applications
      IqClientFactory.getIqClient
          { it.credentialsId == credentialsId && it.serverUrl == URI.create(serverUrl) } >> client
      def configuration = (NxiqConfiguration.DescriptorImpl) jenkins.getInstance().
          getDescriptor(NxiqConfiguration.class)

    and:
      FormValidation validation = configuration.doVerifyCredentials(serverUrl, credentialsId)

    then:
      validation.kind == Kind.OK
      validation.message == "Nexus IQ Server connection succeeded (2 applications)"

    where:
      serverUrl << ['serverUrl']
      credentialsId << ['credentialsId']
      applications << [
        [
            [
                id: 'project-1-id',
                publicId: 'project-1-public-id',
                name: 'Test Project 1'
            ],
            [
                id: 'project-2-id',
                publicId: 'project-2-public-id',
                name: 'Test Project 2'
            ]
        ]
      ]
  }

  def 'it tests invalid server credentials'() {
    when:
      GroovyMock(IqClientFactory.class, global: true)
      def client = Mock(InternalIqClient.class)
      client.getApplicationsForApplicationEvaluation() >> { throw new IqClientException("something went wrong") }
      IqClientFactory.getIqClient
          { it.credentialsId == credentialsId && it.serverUrl == URI.create(serverUrl) } >> client
      def configuration = (NxiqConfiguration.DescriptorImpl) jenkins.getInstance().
          getDescriptor(NxiqConfiguration.class)

    and:
      FormValidation validation = configuration.doVerifyCredentials(serverUrl, credentialsId)


    then:
      validation.kind == Kind.ERROR
      validation.message.startsWith("Nexus IQ Server connection failed")

    where:
      serverUrl << ['serverUrl']
      credentialsId << ['credentialsId']
  }

  def saveConfig(String id, String displayName) {
    def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
    globalConfiguration.iqConfigs = []
    globalConfiguration.iqConfigs.add(new NxiqConfiguration(id, 'internalId', displayName, 'http://foo.com', 'credId', false))
    globalConfiguration.save()
  }

  NxiqConfiguration.DescriptorImpl getDescriptor() {
    (NxiqConfiguration.DescriptorImpl) jenkins.getInstance().getDescriptor(NxiqConfiguration.class)
  }
}
