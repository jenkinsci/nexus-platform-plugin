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

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.Nxrm2Configuration
import org.sonatype.nexus.ci.config.Nxrm3Configuration

import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

class Nxrm2UtilTest
    extends Specification
{
  @Rule
  protected JenkinsRule jenkins = new JenkinsRule()

  def client = Mock(RepositoryManagerV2Client.class)

  def setup() {
    GroovyMock(RepositoryManagerClientUtil.class, global: true)
    RepositoryManagerClientUtil.nexus2Client(_, _) >> client
  }

  def 'getsReposUsingValidInstance'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.nxrmConfigs = []
      globalConfiguration.nxrmConfigs.
          add(new Nxrm2Configuration('id', 'internalId', 'displayName', 'http://foo.com', 'credId'))
      globalConfiguration.save()

      client.getRepositoryList() >> repositories
    when:
      def fetchedRepos = Nxrm2Util.getApplicableRepositories('id')

    then:
      fetchedRepos.size() > 0

    where:
      repositories << [
          [
              [
                  id              : 'maven-releases',
                  name            : 'Maven Releases',
                  format          : 'maven2',
                  repositoryType  : 'hosted',
                  repositoryPolicy: 'Release'
              ]
          ]
      ]
  }

  def 'failsUsingInvalidInstance'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.nxrmConfigs = []
      globalConfiguration.nxrmConfigs.
          add(new Nxrm3Configuration('id', 'internalId', 'displayName', 'http://foo.com', 'credId'))
      globalConfiguration.save()

    when:
      def actualRepos = Nxrm2Util.getApplicableRepositories('id')

    then:
      def thrown = thrown(IllegalArgumentException)
      thrown.message == 'Specified Nexus Repository Manager instance is not a 2.x server'
  }

  def 'getsOnlyMaven2HostedReleaseRepos'() {
    setup:
      client.getRepositoryList() >> repositories

    when:
      def fetchedRepos = Nxrm2Util.getApplicableRepositories('foo', 'bar')

    then:
      fetchedRepos.size() == 2
      fetchedRepos.equals([
          [
              id              : 'maven-releases',
              name            : 'Maven Releases',
              format          : 'maven2',
              repositoryType  : 'hosted',
              repositoryPolicy: 'Release'
          ],
          [
              id              : 'other-maven-releases',
              name            : 'Other Maven Releases',
              format          : 'maven2',
              repositoryType  : 'hosted',
              repositoryPolicy: 'Release'
          ]
      ])

    where:
      repositories << [
          [
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
      ]
  }
}
