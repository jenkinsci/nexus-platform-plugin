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

class IqConfigNoIdToIdMigratorTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  def migrateGlobalConfigurationIqConfigsToHaveIds() {
    setup:
      def globalNexusConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalNexusConfiguration.iqConfigs = [
          new NxiqConfiguration(null, null, null, 'serverUrl', 'credentialsId', false)
      ]
      globalNexusConfiguration.save()

    when:
      IqConfigNoIdToIdMigrator.migrateGlobalConfiguration()

    then:
      globalNexusConfiguration.iqConfigs.size() == 1
      assert globalNexusConfiguration.iqConfigs[0].id
      assert globalNexusConfiguration.iqConfigs[0].internalId
      assert globalNexusConfiguration.iqConfigs[0].displayName
      globalNexusConfiguration.iqConfigs[0].id == globalNexusConfiguration.iqConfigs[0].displayName
      globalNexusConfiguration.iqConfigs[0].serverUrl == 'serverUrl'
      globalNexusConfiguration.iqConfigs[0].credentialsId == 'credentialsId'
      !globalNexusConfiguration.iqConfigs[0].hideReports
  }

  def migrateGlobalConfigurationIqConfigsToHaveIds_Null() {
    setup:
      def globalNexusConfiguration = GlobalNexusConfiguration.globalNexusConfiguration

    when:
      IqConfigNoIdToIdMigrator.migrateGlobalConfiguration()

    then:
      !globalNexusConfiguration.iqConfigs
  }

  def migrateGlobalConfigurationIqConfigsToHaveIds_Empty() {
    setup:
      def globalNexusConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalNexusConfiguration.iqConfigs = []
      globalNexusConfiguration.save()

    when:
      IqConfigNoIdToIdMigrator.migrateGlobalConfiguration()

    then:
      !globalNexusConfiguration.iqConfigs
  }

  def migrateGlobalConfigurationIqConfigsToHaveIds_SomeNotMigrated() {
    setup:
      def globalNexusConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalNexusConfiguration.iqConfigs = [
          new NxiqConfiguration(null, null, null, 'serverUrl', 'credentialsId', true),
          new NxiqConfiguration('id2', 'internalId', 'displayName', 'serverUrl', 'credentialsId', false)
      ]
      globalNexusConfiguration.save()

    when:
      IqConfigNoIdToIdMigrator.migrateGlobalConfiguration()

    then:
      globalNexusConfiguration.iqConfigs.size() == 2
      assert globalNexusConfiguration.iqConfigs[0].id
      globalNexusConfiguration.iqConfigs[0].internalId == 'internalId'
      assert globalNexusConfiguration.iqConfigs[0].displayName
      globalNexusConfiguration.iqConfigs[0].id == globalNexusConfiguration.iqConfigs[0].displayName
      globalNexusConfiguration.iqConfigs[0].serverUrl == 'serverUrl'
      globalNexusConfiguration.iqConfigs[0].credentialsId == 'credentialsId'
      globalNexusConfiguration.iqConfigs[0].hideReports

      globalNexusConfiguration.iqConfigs[1].id == 'id2'
      globalNexusConfiguration.iqConfigs[1].internalId == 'internalId'
      globalNexusConfiguration.iqConfigs[1].displayName == 'displayName'
      globalNexusConfiguration.iqConfigs[1].serverUrl == 'serverUrl'
      globalNexusConfiguration.iqConfigs[1].credentialsId == 'credentialsId'
      !globalNexusConfiguration.iqConfigs[1].hideReports
  }
}
