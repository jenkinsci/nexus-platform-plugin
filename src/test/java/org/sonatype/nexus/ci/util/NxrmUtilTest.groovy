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

import com.sonatype.nexus.api.repository.v2.RepositoryManagerV2Client
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.Nxrm2Configuration
import org.sonatype.nexus.ci.config.Nxrm3Configuration
import org.sonatype.nexus.ci.config.NxrmConfiguration

import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

class NxrmUtilTest
    extends Specification
{
  static final GLOBAL = [global: true] as Map<String, Object>

  @Rule
  protected JenkinsRule jenkins = new JenkinsRule()

  def 'it populates the list of destination repositories for NXRM3'() {
    setup:
      GroovyMock(GLOBAL, RepositoryManagerClientUtil)
      def nxrmConfiguration = createNxrmConfig()

      def client = Mock(RepositoryManagerV3Client)
      def repositories = [
          [
              name            : 'Maven Releases',
              format          : 'maven2',
              type            : 'hosted',
              repositoryPolicy: 'Release'
          ],
          [
              name            : 'Maven 1 Releases',
              format          : 'maven1',
              type            : 'proxy',
              repositoryPolicy: 'Release'
          ],
          [
              name            : 'Maven Snapshots',
              format          : 'maven2',
              type            : 'hosted',
              repositoryPolicy: 'Snapshot'
          ],
          [
              name            : 'Maven Proxy',
              format          : 'maven2',
              type            : 'proxy',
              repositoryPolicy: 'Release'
          ],
          [
              name            : 'Npm Releases',
              format          : 'npm',
              type            : 'hosted',
              repositoryPolicy: 'Release'
          ]
      ]
      client.getRepositories() >> repositories
      RepositoryManagerClientUtil.nexus3Client(nxrmConfiguration.serverUrl, nxrmConfiguration.credentialsId) >> client

    when: 'retrieve the list of repositories to populate the list box'
      def listBoxModel = NxrmUtil.doFillNexusRepositoryIdItems('nxrm3')

    then: 'ListBox has the correct size'
      //3 in total because of the empty listbox name plus 2 hosted maven repos
      listBoxModel.size() == 3

    and: 'ListBox has empty item'
      listBoxModel.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      listBoxModel.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE

    and: 'ListBox is populated with maven hosted repos only'
      listBoxModel.get(1).name == repositories.get(0).name    //Maven Releases
      listBoxModel.get(1).value == repositories.get(0).name
      listBoxModel.get(2).name == repositories.get(2).name    //Maven Snapshots
      listBoxModel.get(2).value == repositories.get(2).name
  }

  def 'it populates the list of destination repositories for NXRM2'() {
    setup:
      GroovyMock(GLOBAL, RepositoryManagerClientUtil)
      def nxrmConfiguration = createNxrmConfig('nxrm2')

      def client = Mock(RepositoryManagerV2Client)
      def repositories = [
          [
              id              : 'maven-releases',
              name            : 'Maven Releases',
              format          : 'maven2',
              repositoryType  : 'hosted',
              repositoryPolicy: 'Release'
          ],
          [
              id              : 'maven1-releases',
              name            : 'Maven 1 Releases',
              format          : 'maven1',
              repositoryType  : 'hosted',
              repositoryPolicy: 'Release'
          ],
          [
              id              : 'maven-snapshots',
              name            : 'Maven Snapshots',
              format          : 'maven2',
              repositoryType  : 'hosted',
              repositoryPolicy: 'Snapshot'
          ],
          [
              id              : 'maven-proxy',
              name            : 'Maven Proxy',
              format          : 'maven2',
              repositoryType  : 'proxy',
              repositoryPolicy: 'Release'
          ],
          [
              id              : 'other-maven-releases',
              name            : 'Other Maven Releases',
              format          : 'maven2',
              repositoryType  : 'hosted',
              repositoryPolicy: 'Release'
          ]
      ]
      client.getRepositoryList() >> repositories
      RepositoryManagerClientUtil.nexus2Client(nxrmConfiguration.serverUrl, nxrmConfiguration.credentialsId) >> client

    when: 'retrieve the list of repositories to populate the list box'
      def listBoxModel = NxrmUtil.doFillNexusRepositoryIdItems('nxrm2')

    then: 'ListBox has the correct size'
      //3 in total because of the empty listbox name plus 2 maven hosted release repos
      listBoxModel.size() == 3

    and: 'ListBox has empty item'
      listBoxModel.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      listBoxModel.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE

    and: 'ListBox is populated with maven hosted repos only'
      listBoxModel.get(1).name == repositories.get(0).name    //Maven Releases
      listBoxModel.get(1).value == repositories.get(0).id
      listBoxModel.get(2).name == repositories.get(4).name    //Other Maven Releases
      listBoxModel.get(2).value == repositories.get(4).id
  }

  //get a config, defaults to nxrm3
  NxrmConfiguration createNxrmConfig(String id = 'nxrm3') {
    def configurationList = []
    def nxrmConfiguration
    if (id == 'nxrm2') {
      nxrmConfiguration = new Nxrm2Configuration(id, "internal${id}", 'displayName', 'http://foo.com', 'credentialsId')
    } else {
      nxrmConfiguration = new Nxrm3Configuration(id, "internal${id}", 'displayName', 'http://foo.com', 'credentialsId')
    }

    configurationList <<  nxrmConfiguration

    def globalConfiguration = jenkins.getInstance().getDescriptorByType(GlobalNexusConfiguration)
    globalConfiguration.nxrmConfigs = configurationList
    globalConfiguration.save()

    nxrmConfiguration
  }
}
