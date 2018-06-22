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

import com.sonatype.nexus.api.exception.RepositoryManagerException
import com.sonatype.nexus.api.repository.v2.RepositoryManagerV2Client

import org.sonatype.nexus.ci.config.Nxrm2Configuration.DescriptorImpl
import org.sonatype.nexus.ci.config.NxrmConfiguration.NxrmDescriptor
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import hudson.util.FormValidation
import hudson.util.FormValidation.Kind

class Nxrm2ConfigurationTest
    extends NxrmConfigurationDescriptorTest
{
  def client = Mock(RepositoryManagerV2Client.class)

  def setup() {
    GroovyMock(RepositoryManagerClientUtil.class, global: true)
    RepositoryManagerClientUtil.nexus2Client(_, _) >> client
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

  def 'it tests valid server credentials'() {
    when:
      client.getRepositoryList() >> repositories

    and:
      FormValidation validation = getDescriptor().doVerifyCredentials(serverUrl, credentialsId)

    then:
      validation.kind == Kind.OK
      validation.message == "Nexus Repository Manager 2.x connection succeeded (1 hosted release Maven 2 repositories)"

    where:
      serverUrl << ['serverUrl']
      credentialsId << ['credentialsId']
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
              ]
          ]
      ]
  }

  def 'it tests invalid server credentials'() {
    when:
      client.getRepositoryList() >> { throw new RepositoryManagerException("something went wrong") }

    and:
      FormValidation validation = getDescriptor().doVerifyCredentials(serverUrl, credentialsId)

    then:
      validation.kind == Kind.ERROR
      validation.message.startsWith("Nexus Repository Manager 2.x connection failed")

    where:
      serverUrl << ['serverUrl']
      credentialsId << ['credentialsId']
  }

  @Override
  NxrmConfiguration createConfig(final String id, final String displayName) {
    new Nxrm2Configuration(id, 'internalId', displayName, 'http://foo.com', 'credId')
  }

  @Override
  NxrmDescriptor getDescriptor() {
    (DescriptorImpl)jenkins.getInstance().getDescriptor(Nxrm2Configuration.class)
  }
}
