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
package org.sonatype.nexus.ci.nxrm

import com.sonatype.nexus.api.repository.v2.RepositoryManagerV2Client
import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.util.FormUtil
import org.sonatype.nexus.ci.config.Nxrm2Configuration
import org.sonatype.nexus.ci.config.NxrmConfiguration
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

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

      def client = Mock(RepositoryManagerV2Client.class)
      def repositories = [
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
      client.getRepositoryList() >> repositories
      RepositoryManagerClientUtil.newRepositoryManagerClient(nxrm2Configuration.serverUrl, nxrm2Configuration.credentialsId) >> client

    when: 'nexus repository items are filled'
      def descriptor = getDescriptor()
      def listBoxModel = descriptor.doFillNexusRepositoryIdItems(nxrm2Configuration.id)

    then: 'ListBox has the correct size'
      listBoxModel.size() == 2

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
