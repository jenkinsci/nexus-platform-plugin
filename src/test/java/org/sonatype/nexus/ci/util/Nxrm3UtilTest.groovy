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

import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.Nxrm2Configuration
import org.sonatype.nexus.ci.config.Nxrm3Configuration

import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

class Nxrm3UtilTest
    extends Specification
{
  @Rule
  protected JenkinsRule jenkins = new JenkinsRule()

  def client = Mock(RepositoryManagerV3Client.class)

  def setup() {
    GroovyMock(RepositoryManagerClientUtil.class, global: true)
    RepositoryManagerClientUtil.nexus3Client(_) >> client
    RepositoryManagerClientUtil.nexus3Client(_, _) >> client
  }

  def 'getsReposUsingValidInstance'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.nxrmConfigs = []
      globalConfiguration.nxrmConfigs.
          add(new Nxrm3Configuration('id', 'internalId', 'displayName', 'http://foo.com', 'credId'))
      globalConfiguration.save()

      client.getRepositories() >> repositories
    when:
      def fetchedRepos = Nxrm3Util.getApplicableRepositories('id')

    then:
      fetchedRepos.size() > 0

    where:
      repositories << [
          [
              [
                  name  : 'Maven Releases',
                  format: 'maven2',
                  type  : 'hosted',
                  url   : 'http://foo.com/repository/maven-releases'
              ]
          ]
      ]
  }

  def 'failsUsingInvalidInstance'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.nxrmConfigs = []
      globalConfiguration.nxrmConfigs.
          add(new Nxrm2Configuration('id', 'internalId', 'displayName', 'http://foo.com', 'credId'))
      globalConfiguration.save()

    when:
      def actualRepos = Nxrm3Util.getApplicableRepositories('id')

    then:
      def thrown = thrown(IllegalArgumentException)
      thrown.message == 'Specified Nexus Repository Manager instance is not a 3.x server'
  }

  def 'getAllHostedRepos'() {
    setup:
      client.getRepositories() >> repositories

    when:
      def fetchedRepos = Nxrm3Util.getApplicableRepositories('foo', 'bar')

    then:
      fetchedRepos.size() == 5
      fetchedRepos.equals([
          [
              name  : 'Maven Releases',
              format: 'maven2',
              type  : 'hosted',
              url   : 'http://foo.com/repository/maven-releases'
          ],
          [
              name  : 'Maven Snapshots',
              format: 'maven2',
              type  : 'hosted',
              url   : 'http://foo.com/repository/maven-snapshots'
          ],
          [
              name  : 'Maven 1 Releases',
              format: 'maven1',
              type  : 'hosted',
              url   : 'http://foo.com/repository/maven-1-releases'
          ],
          [
              name  : 'Npm Releases',
              format: 'npm',
              type  : 'hosted',
              url   : 'http://foo.com/repository/npm-releases'
          ],
          [
              name  : 'Raw Hosted',
              format: 'raw',
              type  : 'hosted',
              url   : 'http://foo.com/repository/raw-hosted'

          ]
      ])

    where:
      repositories << [
          [
              [
                  name  : 'Maven Releases',
                  format: 'maven2',
                  type  : 'hosted',
                  url   : 'http://foo.com/repository/maven-releases'
              ],
              [
                  name  : 'Maven Snapshots',
                  format: 'maven2',
                  type  : 'hosted',
                  url   : 'http://foo.com/repository/maven-snapshots'
              ],
              [
                  name  : 'Maven 1 Releases',
                  format: 'maven1',
                  type  : 'hosted',
                  url   : 'http://foo.com/repository/maven-1-releases'
              ],
              [
                  name  : 'Npm Releases',
                  format: 'npm',
                  type  : 'hosted',
                  url   : 'http://foo.com/repository/npm-releases'
              ],
              [
                  name  : 'SomeNugetProxy',
                  format: 'nuget',
                  type  : 'proxy',
                  url   : 'http://foo.com/repository/some-nuget-proxy'
              ],
              [
                  name  : 'Proxy Maven',
                  format: 'maven2',
                  type  : 'proxy',
                  url   : 'http://foo.com/repository/proxy-maven'
              ],
              [
                  name  : 'Raw Hosted',
                  format: 'raw',
                  type  : 'hosted',
                  url   : 'http://foo.com/repository/raw-hosted'

              ]
          ]
      ]
  }

  def 'getsOnlyMaven2HostedRepos'() {
    setup:
      client.getRepositories() >> repositories

    when:
      def fetchedRepos = Nxrm3Util.getApplicableRepositories('foo', 'bar', 'maven2')

    then:
      fetchedRepos.size() == 3
      fetchedRepos.equals([
          [
              name  : 'Maven Releases',
              format: 'maven2',
              type  : 'hosted',
              url   : 'http://foo.com/repository/maven-releases'
          ],
          [
              name  : 'Maven Snapshots',
              format: 'maven2',
              type  : 'hosted',
              url   : 'http://foo.com/repository/maven-snapshots'
          ],
          [
              name  : 'Other Maven Releases',
              format: 'maven2',
              type  : 'hosted',
              url   : 'http://foo.com/repository/other-maven-releases'
          ]
      ])

    where:
      repositories << [
          [
              [
                  name  : 'Maven Releases',
                  format: 'maven2',
                  type  : 'hosted',
                  url   : 'http://foo.com/repository/maven-releases'
              ],
              [
                  name  : 'Maven Snapshots',
                  format: 'maven2',
                  type  : 'hosted',
                  url   : 'http://foo.com/repository/maven-snapshots'
              ],
              [
                  name  : 'Maven 1 Releases',
                  format: 'maven1',
                  type  : 'hosted',
                  url   : 'http://foo.com/repository/maven-1-releases'
              ],
              [
                  name  : 'Npm Releases',
                  format: 'npm',
                  type  : 'hosted',
                  url   : 'http://foo.com/repository/npm-releases'
              ],
              [
                  name  : 'Other Maven Releases',
                  format: 'maven2',
                  type  : 'hosted',
                  url   : 'http://foo.com/repository/other-maven-releases'
              ],
              [
                  name  : 'Proxy Maven',
                  format: 'maven2',
                  type  : 'proxy',
                  url   : 'http://foo.com/repository/proxy-maven'
              ]
          ]
      ]
  }

  def 'getsOnlyNpmHostedRepos'() {
    setup:
      client.getRepositories() >> repositories

    when:
      def fetchedRepos = Nxrm3Util.getApplicableRepositories('foo', 'bar', 'npm')

    then:
      fetchedRepos.size() == 1
      fetchedRepos.equals([
          [
              name  : 'Npm Releases',
              format: 'npm',
              type  : 'hosted',
              url   : 'http://foo.com/repository/npm-releases'
          ]
      ])

    where:
      repositories << [
          [
              [
                  name  : 'Maven Releases',
                  format: 'maven2',
                  type  : 'hosted',
                  url   : 'http://foo.com/repository/maven-releases'
              ],
              [
                  name  : 'Maven Snapshots',
                  format: 'maven2',
                  type  : 'hosted',
                  url   : 'http://foo.com/repository/maven-snapshots'
              ],
              [
                  name  : 'Maven 1 Releases',
                  format: 'maven1',
                  type  : 'hosted',
                  url   : 'http://foo.com/repository/maven-1-releases'
              ],
              [
                  name  : 'Npm Releases',
                  format: 'npm',
                  type  : 'hosted',
                  url   : 'http://foo.com/repository/npm-releases'
              ],
              [
                  name  : 'Other Maven Releases',
                  format: 'maven2',
                  type  : 'hosted',
                  url   : 'http://foo.com/repository/other-maven-releases'
              ],
              [
                  name  : 'Proxy Maven',
                  format: 'maven2',
                  type  : 'proxy',
                  url   : 'http://foo.com/repository/proxy-maven'
              ]
          ]
      ]
  }
}
