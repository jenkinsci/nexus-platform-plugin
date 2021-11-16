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
      assert globalNexusConfiguration.iqConfigs[0].internalId
      globalNexusConfiguration.iqConfigs[0].id == 'NexusIQServer'
      globalNexusConfiguration.iqConfigs[0].displayName == 'Nexus IQ Server'
      globalNexusConfiguration.iqConfigs[0].serverUrl == 'serverUrl'
      globalNexusConfiguration.iqConfigs[0].credentialsId == 'credentialsId'
      !globalNexusConfiguration.iqConfigs[0].hideReports
  }

  def migrateGlobalConfigurationIqConfigsToHaveIds_AlreadyMigrated() {
    setup:
      def globalNexusConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalNexusConfiguration.iqConfigs = [
          new NxiqConfiguration('testId', 'testInternalId', 'Test DisplayName', 'serverUrl', 'credentialsId', false)
      ]
      globalNexusConfiguration.save()

    when:
      IqConfigNoIdToIdMigrator.migrateGlobalConfiguration()

    then:
      globalNexusConfiguration.iqConfigs.size() == 1
      globalNexusConfiguration.iqConfigs[0].internalId == 'testInternalId'
      globalNexusConfiguration.iqConfigs[0].id == 'testId'
      globalNexusConfiguration.iqConfigs[0].displayName == 'Test DisplayName'
      globalNexusConfiguration.iqConfigs[0].serverUrl == 'serverUrl'
      globalNexusConfiguration.iqConfigs[0].credentialsId == 'credentialsId'
      !globalNexusConfiguration.iqConfigs[0].hideReports
  }

  def migrateGlobalConfigurationIqConfigsToHaveIds_NullIqConfigs() {
    setup:
      def globalNexusConfiguration = GlobalNexusConfiguration.globalNexusConfiguration

    when:
      IqConfigNoIdToIdMigrator.migrateGlobalConfiguration()

    then:
      !globalNexusConfiguration.iqConfigs
  }

  def migrateGlobalConfigurationIqConfigsToHaveIds_EmptyIqConfigs() {
    setup:
      def globalNexusConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalNexusConfiguration.iqConfigs = []
      globalNexusConfiguration.save()

    when:
      IqConfigNoIdToIdMigrator.migrateGlobalConfiguration()

    then:
      !globalNexusConfiguration.iqConfigs
  }
}
