/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.util

import com.sonatype.nexus.api.iq.ApplicationSummary
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

  def 'doFillIqApplicationItems populates empty list when no server is configured'() {
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

      iqClient.applicationsForApplicationEvaluation >> []

    when: 'doFillIqApplicationItems is called with specific credentialsId'
      def applicationItems = IqUtil.doFillIqApplicationItems('jobCredentialsId')

    then:
      applicationItems.size() == 1
      applicationItems.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      applicationItems.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE

  }
}
