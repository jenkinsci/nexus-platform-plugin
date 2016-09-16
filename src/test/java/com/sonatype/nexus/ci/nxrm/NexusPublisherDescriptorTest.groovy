/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm

import com.sonatype.nexus.api.repository.RepositoryManagerClient
import com.sonatype.nexus.ci.config.GlobalNexusConfiguration
import com.sonatype.nexus.ci.config.Nxrm2Configuration
import com.sonatype.nexus.ci.config.NxrmConfiguration
import com.sonatype.nexus.ci.util.FormUtil
import com.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

abstract class NexusPublisherDescriptorTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  abstract NexusPublisherDescriptor getDescriptor()

  def 'it populates Nexus instances'() {
    setup:
      def nxrm2Configuration = saveGlobalConfigurationWithNxrm2Configuration()

    when: 'nexus instance items are filled'
      def descriptor = getDescriptor()
      def listBoxModel = descriptor.doFillNexusInstanceIdItems()

    then: 'ListBox has the correct size'
      listBoxModel.size() == 2

    and: 'ListBox has empty item'
      listBoxModel.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      listBoxModel.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE

    and: 'ListBox is populated'
      listBoxModel.get(1).name == nxrm2Configuration.displayName
      listBoxModel.get(1).value == nxrm2Configuration.id
  }

  def 'it populates Nexus repositories'() {
    setup:
      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      def nxrm2Configuration = saveGlobalConfigurationWithNxrm2Configuration()

      def client = Mock(RepositoryManagerClient.class)
      def repositories = [
          [
              id  : 'maven-releases',
              name: 'Maven Releases',
              format: 'maven2'
          ],
          [
              id  : 'maven-snapshots',
              name: 'Maven Snapshots',
              format: 'maven1'
          ],
          [
              id  : 'nuget-releases',
              name: 'NuGet Releases',
              format: 'nuget'
          ]
      ]
      client.getRepositoryList() >> repositories
      RepositoryManagerClientUtil.buildRmClient(nxrm2Configuration.serverUrl, nxrm2Configuration.credentialsId) >> client

    when: 'nexus repository items are filled'
      def descriptor = getDescriptor()
      def listBoxModel = descriptor.doFillNexusRepositoryIdItems(nxrm2Configuration.id)

    then: 'ListBox has the correct size'
      listBoxModel.size() == 3

    and: 'ListBox has empty item'
      listBoxModel.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      listBoxModel.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE

    and: 'ListBox is populated'
      listBoxModel.get(1).name == repositories.get(0).name
      listBoxModel.get(1).value == repositories.get(0).id
  }

  protected Nxrm2Configuration saveGlobalConfigurationWithNxrm2Configuration() {
    def configurationList = new ArrayList<NxrmConfiguration>()
    def nxrm2Configuration = new Nxrm2Configuration('id', 'internalId', 'displayName', 'http://foo.com', 'credentialsId')
    configurationList.push(nxrm2Configuration)

    def globalConfiguration = jenkins.getInstance().getDescriptorByType(GlobalNexusConfiguration.class)
    globalConfiguration.nxrmConfigs = configurationList
    globalConfiguration.save()

    return nxrm2Configuration
  }
}
