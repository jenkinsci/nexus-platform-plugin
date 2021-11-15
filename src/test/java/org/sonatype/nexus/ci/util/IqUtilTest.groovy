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
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id1', 'internalId1', 'displayName1', 'serverUrl1',
          'credentialsId1', false))
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id2', 'internalId2', 'displayName2', 'serverUrl2',
          'credentialsId2', false))
      globalConfiguration.save()

    when: 'doFillIqApplicationItems is called'
      def iqConfigurations = IqUtil.getIqConfigurations()

    then:
      iqConfigurations.size() == 2
      iqConfigurations.get(0).id == 'id1'
      iqConfigurations.get(1).id == 'id2'
  }

  def 'hasIqConfiguration returns true if there is an IQ configuration'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id', 'internalId', 'displayName', 'serverUrl',
          'credentialsId', false))
      globalConfiguration.save()

    when: 'doFillIqApplicationItems is called'
      def result = IqUtil.hasIqConfiguration()

    then:
      result
  }

  def 'hasIqConfiguration returns false if there is no IQ configuration'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.save()

    when: 'doFillIqApplicationItems is called'
      def result = IqUtil.hasIqConfiguration()

    then:
      !result
  }

  def 'getIqConfiguration returns the IQ configuration matching the given IQ Instance id'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id1', 'internalId1', 'displayName1', 'serverUrl1',
          'credentialsId1', false))
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id2', 'internalId2', 'displayName2', 'serverUrl2',
          'credentialsId2', false))
      globalConfiguration.save()

    when: 'doFillIqApplicationItems is called'
      def iqConfiguration = IqUtil.getIqConfiguration('id1')

    then:
      assert iqConfiguration
      iqConfiguration.id == 'id1'
  }
  
  def 'getIqConfiguration returns null if no IQ configuration matches the given IQ Instance id'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id1', 'internalId1', 'displayName1', 'serverUrl1',
          'credentialsId1', false))
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id2', 'internalId2', 'displayName2', 'serverUrl2',
          'credentialsId2', false))
      globalConfiguration.save()

    when: 'doFillIqApplicationItems is called'
      def iqConfiguration = IqUtil.getIqConfiguration('id3')

    then:
      !iqConfiguration
  }
  
  def 'getFirstIqConfiguration returns the first IQ configuration'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id1', 'internalId1', 'displayName1', 'serverUrl1',
          'credentialsId1', false))
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id2', 'internalId2', 'displayName2', 'serverUrl2',
          'credentialsId2', false))
      globalConfiguration.save()

    when: 'doFillIqApplicationItems is called'
      def iqConfiguration = IqUtil.getFirstIqConfiguration()

    then:
      assert iqConfiguration
      iqConfiguration.id == 'id1'
  }
  
  def 'getFirstIqConfiguration returns null if there are no IQ configurations'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.save()

    when: 'doFillIqApplicationItems is called'
      def iqConfiguration = IqUtil.getFirstIqConfiguration()

    then:
      !iqConfiguration
  }

  def 'doFillIqApplicationItems populates Iq Application items'() {
    setup:
      final String serverUrl = 'http://localhost/'
      final String credentialsId = 'credentialsId'

      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration('id', 'internalId', 'displayName', serverUrl, credentialsId, false)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

      GroovyMock(IqClientFactory, global: true)
      def iqClient = Mock(InternalIqClient)
      IqClientFactory.getIqClient { it.credentialsId == credentialsId && it.context == job } >> iqClient

      iqClient.applicationsForApplicationEvaluation >> [
          new ApplicationSummary('id1', 'publicId1', 'name1'),
          new ApplicationSummary('id2', 'publicId2', 'name2')
      ]

    when: 'doFillIqApplicationItems is called'
      def applicationItems = IqUtil.doFillIqApplicationItems(serverUrl, credentialsId, job)

    then:
      applicationItems.size() == 3
      applicationItems.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      applicationItems.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE
      applicationItems.get(1).name == 'name1'
      applicationItems.get(1).value == 'publicId1'
      applicationItems.get(2).name == 'name2'
      applicationItems.get(2).value == 'publicId2'
  }

  def 'doFillIqApplicationItems returns list with empty options no server is configured'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.save()

    when: 'doFillIqApplicationItems is called'
      def applicationItems = IqUtil.doFillIqApplicationItems(null, 'credentialsId', job)

    then:
      applicationItems.size() == 1
      applicationItems.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      applicationItems.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE
  }

  def 'doFillIqApplicationItems returns list with empty options no credentials is configured'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.save()

    when: 'doFillIqApplicationItems is called'
      def applicationItems = IqUtil.doFillIqApplicationItems('serverUrl', null, job)

    then:
      applicationItems.size() == 1
      applicationItems.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      applicationItems.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE
  }

  def 'doFillIqApplicationItems uses jobSpecificCredentialsId'() {
    setup:
      final String serverUrl = 'http://localhost/'
      final String credentialsId = 'credentialsId'

      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration('id', 'internalId', 'displayName', serverUrl, credentialsId, false)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

      GroovyMock(IqClientFactory, global: true)
      def iqClient = Mock(InternalIqClient)

      IqClientFactory.getIqClient { it.credentialsId == 'jobCredentialsId' && it.context == job } >> iqClient

      iqClient.applicationsForApplicationEvaluation >> [new ApplicationSummary('id', 'publicId', 'name')]

    when: 'doFillIqApplicationItems is called with specific credentialsId'
      def applicationItems = IqUtil.doFillIqApplicationItems(serverUrl, 'jobCredentialsId', job)

    then:
      applicationItems.size() == 2
      applicationItems.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      applicationItems.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE
      applicationItems.get(1).name == 'name'
      applicationItems.get(1).value == 'publicId'
  }

  def 'doFillIqInstanceIdItems populates IQ Instance id items'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id1', 'internalId1', 'displayName1', 'serverUrl1',
          'credentialsId1', false))
      globalConfiguration.iqConfigs.add(new NxiqConfiguration('id2', 'internalId2', 'displayName2', 'serverUrl2',
          'credentialsId2', false))
      globalConfiguration.save()

    when: 'doFillIqInstanceIdItems is called'
      def iqInstanceIdItems = IqUtil.doFillIqInstanceIdItems()

    then:
      iqInstanceIdItems.size() == 3
      iqInstanceIdItems.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      iqInstanceIdItems.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE
      iqInstanceIdItems.get(1).name == 'displayName1'
      iqInstanceIdItems.get(1).value == 'id1'
      iqInstanceIdItems.get(2).name == 'displayName2'
      iqInstanceIdItems.get(2).value == 'id2'
  }

  def 'doFillIqStageItems populates stage items'() {
    setup:
      final String serverUrl = 'http://localhost/'
      final String credentialsId = 'credentialsId'

      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration('id', 'internalId', 'displayName', serverUrl, credentialsId, false)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

      GroovyMock(IqClientFactory, global: true)
      def iqClient = Mock(InternalIqClient)
      IqClientFactory.getIqClient { it.credentialsId == credentialsId && it.context == job } >> iqClient

      iqClient.getLicensedStages(_) >> [
          new Stage('id1', 'build'),
          new Stage('id2', 'operate')
      ]

    when: 'doFillIqStageItems is called'
      def stageItems = IqUtil.doFillIqStageItems(serverUrl, credentialsId, job)

    then:
      stageItems.size() == 3
      stageItems.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      stageItems.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE
      stageItems.get(1).name == 'build'
      stageItems.get(1).value == 'id1'
      stageItems.get(2).name == 'operate'
      stageItems.get(2).value == 'id2'
  }

  def 'doFillIqStageItems uses jobSpecificCredentialsId'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.save()

    when: 'doFillIqStageItems is called'
      def stageItems = IqUtil.doFillIqStageItems(null, '', job)

    then:
      stageItems.size() == 1
      stageItems.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      stageItems.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE
  }

  def 'doFillIqStageItems returns list with empty options when no server is configured'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.save()

      GroovyMock(IqClientFactory, global: true)
      def iqClient = Mock(InternalIqClient)
      IqClientFactory.getIqClient(
          new IqClientFactoryConfiguration(credentialsId: 'jobCredentialsId', context: job)) >> iqClient

      iqClient.getLicensedStages(_) >> [
          new Stage('id1', 'build'),
          new Stage('id2', 'operate')
      ]

    when: 'doFillIqStageItems is called'
      def stageItems = IqUtil.doFillIqStageItems(null, 'jobCredentialsId', job)

    then:
      stageItems.size() == 1
      stageItems.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      stageItems.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE
  }

  def 'doFillIqStageItems returns list with empty options when no credentials is configured'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = []
      globalConfiguration.save()

      GroovyMock(IqClientFactory, global: true)
      def iqClient = Mock(InternalIqClient)
      IqClientFactory.getIqClient(
          new IqClientFactoryConfiguration(credentialsId: 'jobCredentialsId', context: job)) >> iqClient

      iqClient.getLicensedStages(_) >> [
          new Stage('id1', 'build'),
          new Stage('id2', 'operate')
      ]

    when: 'doFillIqStageItems is called'
      def stageItems = IqUtil.doFillIqStageItems('serverUrl', null, job)

    then:
      stageItems.size() == 1
      stageItems.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      stageItems.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE
  }

  def 'calls IqUtil with the correct arguments'() {
    setup:
      GroovyMock(IqClientFactory, global: true)
      final String serverUrl = 'http://localhost/'
      final String globalCredentialsId = 'globalCredentialsId'


      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration('id', 'internalId', 'displayName', serverUrl, globalCredentialsId,
          false)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

      GroovyMock(IqClientFactory, global: true)
      def iqClient = Mock(InternalIqClient)
      iqClient.getApplicationsForApplicationEvaluation() >> []
      IqClientFactory.getIqClient { it.credentialsId == credentialsId && it.context == job } >> iqClient

    when:
      def validation = IqUtil.verifyJobCredentials(serverUrl, creds, job)

    then:
      validation.kind == Kind.OK

    where:
      creds       | credentialsId
      'creds-123' | 'creds-123'
  }
}
