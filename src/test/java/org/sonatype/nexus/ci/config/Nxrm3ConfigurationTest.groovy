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
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client

import org.sonatype.nexus.ci.config.Nxrm3Configuration.DescriptorImpl
import org.sonatype.nexus.ci.config.NxrmConfiguration.NxrmDescriptor
import org.sonatype.nexus.ci.util.Nxrm3Util
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import hudson.util.FormValidation
import hudson.util.FormValidation.Kind

class Nxrm3ConfigurationTest
    extends NxrmConfigurationDescriptorTest
{
  RepositoryManagerV3Client client

  def setup() {
    client = Mock(RepositoryManagerV3Client.class)
    GroovyMock(RepositoryManagerClientUtil.class, global: true)
    RepositoryManagerClientUtil.nexus3Client(_, _) >> client
  }

  def 'it checks nxrm version'() {
    when:
      "checking $serverUrl for nxrm version"
      client.getVersion() >> new com.sonatype.nexus.api.repository.v3.NxrmVersion(version, edition)
      client.getRepositories() >> repositories
      def validation = descriptor.doVerifyCredentials(serverUrl, credentialsId)

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml().contains(message)

    cleanup:
      GroovySystem.metaClassRegistry.setMetaClass(URL, null)

    where:
      kind         | serverUrl               | credentialsId   | version | edition | message  | repositories
      Kind.WARNING | 'http://nxrm.valid.com' | 'credentialsId' | '3.12.1'| 'PRO'   |
          'NXRM PRO 3.12.1 found. Some operations require Nexus Repository Manager ' +
          'Professional server version 3.13.0 or newer; use of an incompatible server could result in failed builds.' |
          []
      Kind.WARNING | 'http://nxrm.valid.com' | 'credentialsId' | '3.13.0'| 'OSS'   |
          'NXRM OSS 3.13.0 found. Some operations require Nexus Repository Manager ' +
          'Professional server version 3.13.0 or newer; use of an incompatible server could result in failed builds.' |
          []
      Kind.OK      | 'http://nxrm.valid.com/'| 'credentialsId' | '3.13.0'| 'PRO'   |
          'Nexus Repository Manager 3.x connection succeeded (1 hosted maven2 repositories)' |
          [
              [
                  url   : 'maven-releases',
                  name  : 'Maven Releases',
                  format: 'maven2',
                  type  : 'hosted'
              ]
          ]
  }

  def 'it checks nxrm version for invalid urls'() {
    when:
      "checking $serverUrl for nxrm version"
      client.getVersion() >> { throw new RepositoryManagerException("something went wrong") }
      def validation = descriptor.doVerifyCredentials(serverUrl, credentialsId)

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml().contains(message)

    cleanup:
      GroovySystem.metaClassRegistry.setMetaClass(URL, null)

    where:
      kind       | serverUrl        | credentialsId   | message
      Kind.ERROR | 'http://foo.com' | 'credentialsId' | 'Nexus Repository Manager 3.x connection failed'
  }

  def 'it tests valid server credentials'() {
    when:
      client.getRepositories() >> repositories
      client.getVersion() >> new com.sonatype.nexus.api.repository.v3.NxrmVersion("3.13.0", "PRO")

    and:
      FormValidation validation = descriptor.doVerifyCredentials(serverUrl, credentialsId)

    then:
      validation.kind == Kind.OK
      validation.message == "Nexus Repository Manager 3.x connection succeeded (2 hosted maven2 repositories)"

    where:
      serverUrl << ['serverUrl']
      credentialsId << ['credentialsId']
      repositories << [
          [
              [
                  url   : 'maven-releases',
                  name  : 'Maven Releases',
                  format: 'maven2',
                  type  : 'hosted'
              ],
              [
                  url   : 'maven1-releases',
                  name  : 'Maven 1 Releases',
                  format: 'maven1',
                  type  : 'hosted'
              ],
              [
                  url   : 'maven-snapshots',
                  name  : 'Maven Snapshots',
                  format: 'maven2',
                  type  : 'hosted'
              ],
              [
                  url   : 'maven-proxy',
                  name  : 'Maven Proxy',
                  format: 'maven2',
                  type  : 'proxy'
              ]
          ]
      ]
  }

  def 'it tests invalid server credentials'() {
    when:
      client.getRepositories() >> { throw new RepositoryManagerException("something went wrong") }

    and:
      FormValidation validation = descriptor.doVerifyCredentials(serverUrl, credentialsId)

    then:
      validation.kind == Kind.ERROR
      validation.message.startsWith("Nexus Repository Manager 3.x connection failed")

    where:
      serverUrl << ['serverUrl']
      credentialsId << ['credentialsId']
  }

  def 'defaults to anonymous access with no credentials'() {
    when:
      GroovySpy(Nxrm3Util.class, global: true)
      client.getRepositories() >> []
    and:
      descriptor.doVerifyCredentials(serverUrl, credentialsId)

    then:
      1 * Nxrm3Util.getApplicableRepositories(serverUrl, null, 'maven2')

    where:
      serverUrl << ['serverUrl']
      credentialsId << [null]
  }

  @Override
  NxrmConfiguration createConfig(final String id, final String displayName) {
    new Nxrm3Configuration(id, 'internalId', displayName, 'http://foo.com', 'credId')
  }

  @Override
  NxrmDescriptor getDescriptor() {
    (DescriptorImpl) jenkins.getInstance().getDescriptor(Nxrm3Configuration.class)
  }
}
