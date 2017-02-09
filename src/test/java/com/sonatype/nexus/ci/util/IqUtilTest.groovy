/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.util

import com.sonatype.nexus.api.iq.ApplicationSummary
import com.sonatype.nexus.api.iq.Stage
import com.sonatype.nexus.api.iq.internal.InternalIqClient
import com.sonatype.nexus.ci.config.GlobalNexusConfiguration
import com.sonatype.nexus.ci.config.NxiqConfiguration
import com.sonatype.nexus.ci.iq.IqClientFactory

import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

public class IqUtilTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  def 'doFillIqApplicationItems populates Iq Application items'() {
    setup:
      final String serverUrl = 'http://localhost/'
      final String credentialsId = 'credentialsId'

      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration(serverUrl, false, credentialsId)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

      GroovyMock(IqClientFactory, global: true)
      def iqClient = Mock(InternalIqClient)
      IqClientFactory.getIqClient('') >> iqClient

      iqClient.applicationsForApplicationEvaluation >> [
          new ApplicationSummary('id1', 'publicId1', 'name1'),
          new ApplicationSummary('id2', 'publicId2', 'name2')
      ]

    when: 'doFillIqApplicationItems is called'
      def applicationItems = IqUtil.doFillIqApplicationItems('')

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
      def applicationItems = IqUtil.doFillIqApplicationItems('')

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
      def nxiqConfiguration = new NxiqConfiguration(serverUrl, false, credentialsId)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

      GroovyMock(IqClientFactory, global: true)
      def iqClient = Mock(InternalIqClient)
      IqClientFactory.getIqClient('jobCredentialsId') >> iqClient

      iqClient.applicationsForApplicationEvaluation >> [new ApplicationSummary('id', 'publicId', 'name')]

    when: 'doFillIqApplicationItems is called with specific credentialsId'
      def applicationItems = IqUtil.doFillIqApplicationItems('jobCredentialsId')

    then:
      applicationItems.size() == 2
      applicationItems.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      applicationItems.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE
      applicationItems.get(1).name == 'name'
      applicationItems.get(1).value == 'publicId'
  }

  def 'doFillIqStageItems populates stage items'() {
    setup:
      final String serverUrl = 'http://localhost/'
      final String credentialsId = 'credentialsId'

      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration(serverUrl, false, credentialsId)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

      GroovyMock(IqClientFactory, global: true)
      def iqClient = Mock(InternalIqClient)
      IqClientFactory.getIqClient('') >> iqClient

      iqClient.getLicensedStages(_) >> [
          new Stage('id1', 'build'),
          new Stage('id2', 'operate')
      ]

    when: 'doFillIqStageItems is called'
      def stageItems = IqUtil.doFillIqStageItems('')

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
      def stageItems = IqUtil.doFillIqStageItems('')

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
      IqClientFactory.getIqClient('jobCredentialsId') >> iqClient

      iqClient.getLicensedStages(_) >> [
          new Stage('id1', 'build'),
          new Stage('id2', 'operate')
      ]

    when: 'doFillIqStageItems is called'
      def stageItems = IqUtil.doFillIqStageItems('jobCredentialsId')

    then:
      stageItems.size() == 1
      stageItems.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      stageItems.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE
  }
}
