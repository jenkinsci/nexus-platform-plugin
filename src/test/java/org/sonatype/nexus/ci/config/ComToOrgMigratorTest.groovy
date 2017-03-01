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

import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

class ComToOrgMigratorTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  def migratesComGlobalConfigurationToOrgGlobalConfiguration() {
    setup:
      def comGlobalNexusConfiguration = new com.sonatype.nexus.ci.config.GlobalNexusConfiguration()
      comGlobalNexusConfiguration.instanceId = 'instanceId'
      comGlobalNexusConfiguration.iqConfigs = [
          new com.sonatype.nexus.ci.config.NxiqConfiguration(serverUrl: 'serverUrl', isPkiAuthentication: false,
              credentialsId: 'credentialsId')
      ]
      comGlobalNexusConfiguration.nxrmConfigs = [
          new com.sonatype.nexus.ci.config.Nxrm2Configuration(id: 'id', internalId: 'internalId',
              displayName: 'displayName', serverUrl: 'serverUrl', credentialsId: 'credentialsId')
      ]
      comGlobalNexusConfiguration.save()

    when:
      ComToOrgMigrator.migrateGlobalConfiguration()

    then:
      def orgGlobalNexusConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      orgGlobalNexusConfiguration.instanceId == 'instanceId'

      orgGlobalNexusConfiguration.iqConfigs.size() == 1
      orgGlobalNexusConfiguration.iqConfigs[0].@serverUrl == 'serverUrl'
      !orgGlobalNexusConfiguration.iqConfigs[0].isPkiAuthentication
      orgGlobalNexusConfiguration.iqConfigs[0].credentialsId == 'credentialsId'

      orgGlobalNexusConfiguration.nxrmConfigs.size() == 1
      orgGlobalNexusConfiguration.nxrmConfigs[0].id == 'id'
      orgGlobalNexusConfiguration.nxrmConfigs[0].internalId == 'internalId'
      orgGlobalNexusConfiguration.nxrmConfigs[0].displayName == 'displayName'
      orgGlobalNexusConfiguration.nxrmConfigs[0].serverUrl == 'serverUrl'
      orgGlobalNexusConfiguration.nxrmConfigs[0].credentialsId == 'credentialsId'
  }

  def migratorDeletesComGlobalConfiguration() {
    setup:
      def comGlobalNexusConfiguration = new com.sonatype.nexus.ci.config.GlobalNexusConfiguration()
      comGlobalNexusConfiguration.save()

    expect:
      comGlobalNexusConfiguration.exists()

    when:
      ComToOrgMigrator.migrateGlobalConfiguration()

    then:
      !comGlobalNexusConfiguration.exists()
  }

  def migratorOnlyRunsWhenComGlobalConfigurationExists() {
    setup:
      def comGlobalNexusConfiguration = new com.sonatype.nexus.ci.config.GlobalNexusConfiguration()
      comGlobalNexusConfiguration.instanceId = 'instanceId'

    when:
      ComToOrgMigrator.migrateGlobalConfiguration()

    then:
      def orgGlobalNexusConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      orgGlobalNexusConfiguration.instanceId != 'instanceId'
  }
}
