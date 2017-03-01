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

import com.sonatype.nexus.api.exception.RepositoryManagerException
import com.sonatype.nexus.api.repository.RepositoryManagerClient

import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

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

  def 'it validates that display name is unique'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxrmConfiguration = new Nxrm2Configuration('id', 'internalId', 'displayName', 'http://foo.com', 'credId')
      globalConfiguration.nxrmConfigs = []
      globalConfiguration.nxrmConfigs.add(nxrmConfiguration)
      globalConfiguration.save()

      def configuration = (Nxrm2Configuration.DescriptorImpl) jenkins.getInstance().
          getDescriptor(Nxrm2Configuration.class)

    when:
      "validating $displayName"
      def validation = configuration.doCheckDisplayName(displayName, 'otherId')

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
    setup:
      def configuration = (Nxrm2Configuration.DescriptorImpl) jenkins.getInstance().
          getDescriptor(Nxrm2Configuration.class)

    when:
      "validating $displayName"
      def validation = configuration.doCheckDisplayName(displayName, 'id')

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
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxrmConfiguration = new Nxrm2Configuration('id', 'internalId', 'displayName', 'http://foo.com', 'credId')
      globalConfiguration.nxrmConfigs = []
      globalConfiguration.nxrmConfigs.add(nxrmConfiguration)
      globalConfiguration.save()

      def configuration = (Nxrm2Configuration.DescriptorImpl) jenkins.getInstance().
          getDescriptor(Nxrm2Configuration.class)

    when:
      "validating $id"
      def validation = configuration.doCheckId(id, 'otherId')

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
    setup:
      def configuration = (Nxrm2Configuration.DescriptorImpl) jenkins.getInstance().
          getDescriptor(Nxrm2Configuration.class)

    when:
      "validating $id"
      def validation = configuration.doCheckId(id, 'id')

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
    setup:
      def configuration = (Nxrm2Configuration.DescriptorImpl) jenkins.getInstance().
          getDescriptor(Nxrm2Configuration.class)

    when:
      "validating $id"
      def validation = configuration.doCheckId(id, 'id')

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
      def configuration = (Nxrm2Configuration.DescriptorImpl) jenkins.getInstance().
          getDescriptor(Nxrm2Configuration.class)

    when:
      "validating $url"
      def validation = configuration.doCheckServerUrl(url)

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml().startsWith(message)

    where:
      url              | kind         | message
      'foo'            | Kind.ERROR   | 'Malformed url (no protocol: foo)'
      'http://foo.com' | Kind.OK      | '<div/>'
  }

  def 'it validates the server url is required'() {
    setup:
      def configuration = (Nxrm2Configuration.DescriptorImpl) jenkins.getInstance().
          getDescriptor(Nxrm2Configuration.class)

    when:
      "validating $url"
      def validation = configuration.doCheckServerUrl(url)

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml().startsWith(message)

    where:
      url                | kind         | message
      ''                 | Kind.ERROR   | 'Server Url is required'
      null               | Kind.ERROR   | 'Server Url is required'
      'http://foo.com'   | Kind.OK      | '<div/>'
  }

  def 'it tests valid server credentials'() {
    when:
      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      def client = Mock(RepositoryManagerClient.class)
      client.getRepositoryList() >> repositories
      RepositoryManagerClientUtil.newRepositoryManagerClient(serverUrl, credentialsId) >> client
      def configuration = (Nxrm2Configuration.DescriptorImpl) jenkins.getInstance().
          getDescriptor(Nxrm2Configuration.class)

    and:
      FormValidation validation = configuration.doVerifyCredentials(serverUrl, credentialsId)

    then:
      validation.kind == Kind.OK
      validation.message == "Nexus Repository Manager 2.x connection succeeded (1 hosted release Maven 2 repositories)"

    where:
      serverUrl << ['serverUrl']
      credentialsId << ['credentialsId']
      repositories << [
        [
            [
                id: 'maven-releases',
                name: 'Maven Releases',
                format: 'maven2',
                repositoryType: 'hosted',
                repositoryPolicy: 'Release'
            ],
            [
                id: 'maven1-releases',
                name: 'Maven 1 Releases',
                format: 'maven1',
                repositoryType: 'hosted',
                repositoryPolicy: 'Release'
            ],
            [
                id: 'maven-snapshots',
                name: 'Maven Snapshots',
                format: 'maven2',
                repositoryType: 'hosted',
                repositoryPolicy: 'Snapshot'
            ],
            [
                id: 'maven-proxy',
                name: 'Maven Proxy',
                format: 'maven2',
                repositoryType: 'proxy',
                repositoryPolicy: 'Release'
            ]
        ]
      ]
  }

  def 'it tests invalid server credentials'() {
    when:
      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      def client = Mock(RepositoryManagerClient.class)
      client.getRepositoryList() >> { throw new RepositoryManagerException("something went wrong") }
      RepositoryManagerClientUtil.newRepositoryManagerClient(serverUrl, credentialsId) >> client
      def configuration = (Nxrm2Configuration.DescriptorImpl) jenkins.getInstance().getDescriptor(Nxrm2Configuration.class)

    and:
      FormValidation validation = configuration.doVerifyCredentials(serverUrl, credentialsId)


    then:
      validation.kind == Kind.ERROR
      validation.message.startsWith("Nexus Repository Manager 2.x connection failed")

    where:
      serverUrl << ['serverUrl']
      credentialsId << ['credentialsId']
  }
}
