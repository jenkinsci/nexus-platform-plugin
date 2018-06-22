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

import org.sonatype.nexus.ci.config.NxrmConfiguration.NxrmDescriptor
import org.sonatype.nexus.ci.util.FormUtil

import hudson.util.FormValidation.Kind
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

/**
 * Defines the common tests to which any subclass of {@link NxrmConfiguration} should pass
 */
abstract class NxrmConfigurationDescriptorTest
    extends Specification
{
  @Rule
  protected JenkinsRule jenkins = new JenkinsRule()

  abstract NxrmConfiguration createConfig(String id, String displayName)

  abstract NxrmDescriptor getDescriptor()

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

  def 'it shows error message for invalid URL'() {
    when:
      "validating $url"
      def validation = descriptor.doCheckServerUrl(url)

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml().startsWith(message)

    where:
      url              | kind         | message
      'foo'            | Kind.ERROR   | 'Malformed url (no protocol: foo)'
      'foo://bar'      | Kind.ERROR   | 'Malformed url (unknown protocol: foo)'
  }

  def 'it validates the server url is required'() {
    when:
      "validating $url"
      def validation = descriptor.doCheckServerUrl(url)

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml().startsWith(message)

    where:
      url                | kind         | message
      ''                 | Kind.ERROR   | 'Server Url is required'
      null               | Kind.ERROR   | 'Server Url is required'
  }

  def 'it loads the credential items'() {
    setup:
      GroovyMock(FormUtil, global: true)

    when:
      descriptor.doFillCredentialsIdItems("serverUrl", "credentialsId")

    then:
      1 * FormUtil.newCredentialsItemsListBoxModel("serverUrl", "credentialsId", null)
  }

  def saveConfig(String id, String displayName) {
    def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
    globalConfiguration.nxrmConfigs = []
    globalConfiguration.nxrmConfigs.add(createConfig(id, displayName))
    globalConfiguration.save()
  }
}
