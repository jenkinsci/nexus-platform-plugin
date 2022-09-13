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
package org.sonatype.nexus.ci.util

import com.sonatype.nexus.api.iq.ApplicationSummary
import com.sonatype.nexus.api.iq.OrganizationSummary
import com.sonatype.nexus.api.iq.Stage
import com.sonatype.nexus.api.iq.internal.InternalIqClient

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.NxiqConfiguration
import org.sonatype.nexus.ci.iq.IqClientFactory
import org.sonatype.nexus.ci.iq.IqClientFactoryConfiguration

import hudson.model.Job
import hudson.util.FormValidation.Kind
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification
import spock.util.mop.ConfineMetaClassChanges

@ConfineMetaClassChanges([IqClientFactory])
class IqUtilTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  Job job = Mock(Job)

  def 'getIqConfigurations returns a list of IQ configurations'() {
    given: 'a set of global IQ configurations'
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id1', 'internalId1', 'displayName1', 'serverUrl1',
          'credentialsId1', false))
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id2', 'internalId2', 'displayName2', 'serverUrl2',
          'credentialsId2', false))
      globalConfiguration.save()

    when: 'getIqConfigurations is called'
      def iqConfigurations = IqUtil.getIqConfigurations()

    then: 'the list of configurations is returned'
      iqConfigurations*.id == ['id1', 'id2']
  }

  def 'hasIqConfiguration returns true if there is an IQ configuration'() {
    given: 'a set of global IQ configurations'
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id', 'internalId', 'displayName', 'serverUrl',
          'credentialsId', false))
      globalConfiguration.save()

    expect: 'hasIqConfiguration is called and returns true'
      IqUtil.hasIqConfiguration()
  }

  def 'hasIqConfiguration returns false if there is no IQ configuration'() {
    given: 'we do not have any global configuration'
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.save()

    expect: 'hasIqConfiguration is called and returns false'
      !IqUtil.hasIqConfiguration()
  }

  def 'getIqConfiguration returns the IQ configuration matching the given IQ Instance id'() {
    given: 'a set of global IQ configurations'
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id1', 'internalId1', 'displayName1', 'serverUrl1',
          'credentialsId1', false))
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id2', 'internalId2', 'displayName2', 'serverUrl2',
          'credentialsId2', false))
      globalConfiguration.save()

    when: 'getIqConfiguration is called'
      def iqConfiguration = IqUtil.getIqConfiguration('id2')

    then: 'the proper configuration is returned'
      assert iqConfiguration
    iqConfiguration.id == 'id2'
    iqConfiguration.internalId == 'internalId2'
  }
  
  def 'getIqConfiguration returns null if no IQ configuration matches the given IQ Instance id'() {
    given: 'a set of global IQ configurations'
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id1', 'internalId1', 'displayName1', 'serverUrl1',
          'credentialsId1', false))
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id2', 'internalId2', 'displayName2', 'serverUrl2',
          'credentialsId2', false))
      globalConfiguration.save()

    when: 'getIqConfiguration is called with an id that do not exists'
      def iqConfiguration = IqUtil.getIqConfiguration('id3')

    then: 'no configuration is returned'
      !iqConfiguration
  }
  
  def 'getFirstIqConfiguration returns the first IQ configuration'() {
    given: 'a set of global IQ configurations'
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id1', 'internalId1', 'displayName1', 'serverUrl1',
          'credentialsId1', false))
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id2', 'internalId2', 'displayName2', 'serverUrl2',
          'credentialsId2', false))
      globalConfiguration.save()

    when: 'getFirstIqConfiguration is called'
      def iqConfiguration = IqUtil.getFirstIqConfiguration()

    then: 'the proper configuration is returned'
      assert iqConfiguration
      iqConfiguration.id == 'id1'
  }
  
  def 'getFirstIqConfiguration returns null if there are no IQ configurations'() {
    given: 'we do not have any global configuration'
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.save()

    when: 'getFirstIqConfiguration is called'
      def iqConfiguration = IqUtil.getFirstIqConfiguration()

    then: 'no configuration is returned'
      !iqConfiguration
  }

  def 'doFillIqApplicationItems populates Iq Application items'() {
    given: 'a set of global IQ configurations'
      final String serverUrl = 'http://localhost/'
      final String credentialsId = 'credentialsId'
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration('id', 'internalId', 'displayName', serverUrl, credentialsId, false)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

    and: 'an IQ client tied to the global configuration'
      GroovyMock(IqClientFactory, global: true)
      def iqClient = Mock(InternalIqClient)
      IqClientFactory.getIqClient { it.credentialsId == credentialsId && it.context == job } >> iqClient

    when: 'doFillIqApplicationItems is called'
      def applicationItems = IqUtil.doFillIqApplicationItems(serverUrl, credentialsId, job, null)

    then: 'it asks iqClient for the list of applications'
      1 * iqClient.applicationsForApplicationEvaluation >> [
          new ApplicationSummary('id1', 'publicId1', 'name1'),
          new ApplicationSummary('id2', 'publicId2', 'name2')
      ]

    then: 'we got back the list of applications along with an empty option'
      applicationItems*.name == [FormUtil.EMPTY_LIST_BOX_NAME, 'name1', 'name2']
      applicationItems*.value == [FormUtil.EMPTY_LIST_BOX_VALUE, 'publicId1', 'publicId2']
  }

  def 'doFillIqApplicationItems populates Iq Application items for an organization'() {
    given: 'a set of global IQ configurations'
      final String serverUrl = 'http://localhost/'
      final String credentialsId = 'credentialsId'
      final String organizationId = 'organizationId'
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration('id', 'internalId', 'displayName', serverUrl, credentialsId, false)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

    and: 'an IQ client tied to the global configuration'
      GroovyMock(IqClientFactory, global: true)
      def iqClient = Mock(InternalIqClient)
      IqClientFactory.getIqClient { it.credentialsId == credentialsId && it.context == job } >> iqClient

    when: 'doFillIqApplicationItems is called'
      def applicationItems = IqUtil.doFillIqApplicationItems(serverUrl, credentialsId, job, organizationId)

    then: 'it asks iqClient for applications to eval by the organization id'
      1 * iqClient.getApplicationsForApplicationEvaluation(organizationId) >> [
          new ApplicationSummary('id1', 'publicId1', 'name1'),
          new ApplicationSummary('id2', 'publicId2', 'name2')
      ]

    and: 'we got back the list of applications along with an empty option'
      applicationItems*.name == [FormUtil.EMPTY_LIST_BOX_NAME, 'name1', 'name2']
      applicationItems*.value == [FormUtil.EMPTY_LIST_BOX_VALUE, 'publicId1', 'publicId2']
  }

  def 'doFillIqApplicationItems returns list with empty options when no server is configured'() {
    given: 'we do not have any global configuration'
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.save()

    when: 'doFillIqApplicationItems is called with an invalid server'
      def applicationItems = IqUtil.doFillIqApplicationItems(null, 'credentialsId', job, null)

    then: 'a list with only the empty option is returned'
      applicationItems.size() == 1
      applicationItems.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      applicationItems.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE
  }

  def 'doFillIqApplicationItems returns list with empty options when no credentials is configured'() {
    given: 'we do not have any global configuration'
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.save()

    when: 'doFillIqApplicationItems is called with invalid credentials'
      def applicationItems = IqUtil.doFillIqApplicationItems('serverUrl', null, job, null)

    then: 'a list with only the empty option is returned'
      applicationItems.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      applicationItems.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE
  }

  def 'doFillIqApplicationItems uses jobSpecificCredentialsId'() {
    given: 'a set of global IQ configurations'
      final String serverUrl = 'http://localhost/'
      final String credentialsId = 'credentialsId'
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration('id', 'internalId', 'displayName', serverUrl, credentialsId, false)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

    and: 'an IQ client tied to the global configuration'
      GroovyMock(IqClientFactory, global: true)
      def iqClient = Mock(InternalIqClient)
      IqClientFactory.getIqClient { it.credentialsId == 'jobCredentialsId' && it.context == job } >> iqClient

    when: 'doFillIqApplicationItems is called with specific credentialsId'
      def applicationItems = IqUtil.doFillIqApplicationItems(serverUrl, 'jobCredentialsId', job, null)

    then: 'it asks iqClient for the list of applications'
      1 * iqClient.applicationsForApplicationEvaluation >> [
          new ApplicationSummary('id', 'publicId', 'name')
      ]

    and: 'we got back the list of applications along with an empty option'
      applicationItems*.name == [FormUtil.EMPTY_LIST_BOX_NAME, 'name']
      applicationItems*.value == [FormUtil.EMPTY_LIST_BOX_VALUE, 'publicId']
  }

  def 'doFillIqInstanceIdItems populates IQ Instance id items'() {
    given: 'a set of global configurations'
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id1', 'internalId1', 'displayName1', 'serverUrl1',
          'credentialsId1', false))
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id2', 'internalId2', 'displayName2', 'serverUrl2',
          'credentialsId2', false))
      globalConfiguration.save()

    when: 'doFillIqInstanceIdItems is called'
      def iqInstanceIdItems = IqUtil.doFillIqInstanceIdItems()

    then: 'we got back the list of instance ids along with an empty option'
      iqInstanceIdItems*.name == [FormUtil.EMPTY_LIST_BOX_NAME, 'displayName1', 'displayName2']
      iqInstanceIdItems*.value == [FormUtil.EMPTY_LIST_BOX_VALUE, 'id1', 'id2']
  }

  def 'doFillIqStageItems populates stage items'() {
    given: 'a set of global IQ configurations'
      final String serverUrl = 'http://localhost/'
      final String credentialsId = 'credentialsId'
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration('id', 'internalId', 'displayName', serverUrl, credentialsId, false)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

    and: 'an IQ client tied to the global configuration'
      GroovyMock(IqClientFactory, global: true)
      def iqClient = Mock(InternalIqClient)
      IqClientFactory.getIqClient { it.credentialsId == credentialsId && it.context == job } >> iqClient

    when: 'doFillIqStageItems is called'
      def stageItems = IqUtil.doFillIqStageItems(serverUrl, credentialsId, job)

    then: 'it asks iqClient for the list of licensed stages'
      1 * iqClient.getLicensedStages(_) >> [
          new Stage('id1', 'build'),
          new Stage('id2', 'operate')
      ]

    and: 'we got back the list of licensed stages along with the an empty option'
      stageItems*.name == [FormUtil.EMPTY_LIST_BOX_NAME, 'build', 'operate']
      stageItems*.value == [FormUtil.EMPTY_LIST_BOX_VALUE, 'id1', 'id2']
  }

  def 'doFillIqStageItems uses jobSpecificCredentialsId'() {
    given: 'we do not have any global configuration'
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.save()

    when: 'doFillIqStageItems is called'
      def stageItems = IqUtil.doFillIqStageItems(null, '', job)

    then: 'we got back the list of with only an empty option'
      stageItems.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      stageItems.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE
  }

  def 'doFillIqStageItems returns list with empty options when no server is configured'() {
    given: 'we do not have any global configuration'
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.save()

    and: 'and an IQ client'
      GroovyMock(IqClientFactory, global: true)
      def iqClient = Mock(InternalIqClient)
      IqClientFactory.getIqClient(
          new IqClientFactoryConfiguration(credentialsId: 'jobCredentialsId', context: job)) >> iqClient

    when: 'doFillIqStageItems is called with an invalid server'
      def stageItems = IqUtil.doFillIqStageItems(null, 'jobCredentialsId', job)

    then: 'it does not ask iqClient for the list of licensed stages'
      0 * iqClient.getLicensedStages(_) >> [
          new Stage('id1', 'build'),
          new Stage('id2', 'operate')
      ]

    and: 'we got back the list with only an empty option'
      stageItems.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      stageItems.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE
  }

  def 'doFillIqStageItems returns list with empty options when no credentials is configured'() {
    given: 'we do not have any global configuration'
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.save()

    and: 'and an IQ client'
      GroovyMock(IqClientFactory, global: true)
      def iqClient = Mock(InternalIqClient)
      IqClientFactory.getIqClient(
          new IqClientFactoryConfiguration(credentialsId: 'jobCredentialsId', context: job)) >> iqClient

    when: 'doFillIqStageItems is called with invalid credentials'
      def stageItems = IqUtil.doFillIqStageItems('serverUrl', null, job)

    then: 'it does not ask iqClient for the list of licensed stages'
      0 * iqClient.getLicensedStages(_) >> [
          new Stage('id1', 'build'),
          new Stage('id2', 'operate')
      ]

    and: 'we got back the list with only an empty option'
      stageItems.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      stageItems.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE
  }

  def 'calls IqUtil with the correct arguments'() {
    given: 'a set of global IQ configurations'
      final String serverUrl = 'http://localhost/'
      final String globalCredentialsId = 'globalCredentialsId'
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration('id', 'internalId', 'displayName', serverUrl, globalCredentialsId,
          false)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

    and: 'an IQ client tied to the global configuration'
      GroovyMock(IqClientFactory, global: true)
      def iqClient = Mock(InternalIqClient)
      iqClient.getApplicationsForApplicationEvaluation() >> []
      IqClientFactory.getIqClient { it.credentialsId == 'creds-123' && it.context == job } >> iqClient

    when: 'verifyJobCredentials is called'
      def validation = IqUtil.verifyJobCredentials(serverUrl, 'creds-123', job)

    then: 'verification is OK'
      validation.kind == Kind.OK
  }

  def 'doFillIqOrganizationItems populates organization items'() {
    given: 'a set of global IQ configurations'
      final String serverUrl = 'http://localhost/'
      final String credentialsId = 'credentialsId'
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration('id', 'internalId', 'displayName', serverUrl, credentialsId, false)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

    and: 'an IQ client tied to the global configuration'
      GroovyMock(IqClientFactory, global: true)
      def iqClient = Mock(InternalIqClient)
      IqClientFactory.getIqClient { it.credentialsId == credentialsId && it.context == job } >> iqClient

    when: 'doFillIqOrganizationItems is called'
      def organizationItems = IqUtil.doFillIqOrganizationItems(serverUrl, credentialsId, job)

    then: 'it asks iqClient for the configured organizations'
      1 * iqClient.getOrganizationsForApplicationEvaluation() >> [
          new OrganizationSummary('id1', 'test-org'),
          new OrganizationSummary('id2', 'test-org-1')
      ]

    and: 'all the configured organizations are returned along with an empty option'
      organizationItems*.name == [FormUtil.EMPTY_LIST_BOX_NAME, 'test-org', 'test-org-1']
      organizationItems*.value == [FormUtil.EMPTY_LIST_BOX_VALUE, 'id1', 'id2']
  }

  def 'doFillIqOrganizationItems returns list with empty options when no server is configured'() {
    given: 'a set of global IQ configurations'
      final String serverUrl = 'http://localhost/'
      final String credentialsId = 'credentialsId'
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration('id', 'internalId', 'displayName', serverUrl, credentialsId, false)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

    and: 'an IQ client tied to the global configuration'
      GroovyMock(IqClientFactory, global: true)
      def iqClient = Mock(InternalIqClient)
      IqClientFactory.getIqClient { it.credentialsId == credentialsId && it.context == job } >> iqClient

    when: 'doFillIqOrganizationItems is called without a server configuration'
      def organizationItems = IqUtil.doFillIqOrganizationItems(null, 'jobCredentialsId', job)

    then: 'it does not ask iqClient for the configured organizations'
      0 * iqClient.getOrganizationsForApplicationEvaluation() >> [
          new OrganizationSummary('id1', 'test-org'),
          new OrganizationSummary('id2', 'test-org-1')
      ]

    and: 'a list with only the empty option is returned'
      organizationItems.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      organizationItems.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE
  }

  def 'doFillIqOrganizationItems returns list with empty options when no credentials is configured'() {
    given: 'a set of global IQ configurations'
      final String serverUrl = 'http://localhost/'
      final String credentialsId = 'credentialsId'
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration('id', 'internalId', 'displayName', serverUrl, credentialsId, false)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

    and: 'an IQ client tied to the global configuration'
      GroovyMock(IqClientFactory, global: true)
      def iqClient = Mock(InternalIqClient)
      IqClientFactory.getIqClient { it.credentialsId == credentialsId && it.context == job } >> iqClient

    when: 'doFillIqOrganizationItems is called without credentials'
      def organizationItems = IqUtil.doFillIqOrganizationItems('serverUrl', null, job)

    then: 'it does not ask iqClient for the configured organizations'
      0 * iqClient.getOrganizationsForApplicationEvaluation() >> [
          new OrganizationSummary('id1', 'test-org'),
          new OrganizationSummary('id2', 'test-org-1')
      ]

    and: 'a list with only the empty option is returned'
      organizationItems.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      organizationItems.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE
  }
}
