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


import org.slf4j.Logger
import org.slf4j.LoggerFactory

@SuppressWarnings('AbcMetric')
class IqConfigNoIdToIdMigrator
{
  private static final Logger LOGGER = LoggerFactory.getLogger(IqConfigNoIdToIdMigrator)

  static void migrateGlobalConfiguration() {
    LOGGER.debug(Messages.IqConfigNoIdToIdMigrator_MigratingGlobalConfiguration())
    GlobalNexusConfiguration globalNexusConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
    List<NxiqConfiguration> iqConfigs = globalNexusConfiguration.iqConfigs
    int updated = 0
    int total = iqConfigs == null ? 0 : iqConfigs.size()
    if (iqConfigs) {
      String internalId = null
      for (NxiqConfiguration nxiqConfiguration : globalNexusConfiguration.iqConfigs) {
        if (nxiqConfiguration.internalId) {
          internalId = nxiqConfiguration.internalId
          break
        }
      }
      if (internalId == null) {
        internalId = GlobalNexusConfiguration.generateRandomId()
      }
      for (NxiqConfiguration nxiqConfiguration : globalNexusConfiguration.iqConfigs) {
        boolean changed = false
        if (nxiqConfiguration.internalId == null) {
          nxiqConfiguration.internalId = internalId
          changed = true
        }
        if (nxiqConfiguration.id == null) {
          nxiqConfiguration.id = GlobalNexusConfiguration.generateRandomId()
          changed = true
        }
        if (nxiqConfiguration.displayName == null) {
          nxiqConfiguration.displayName = nxiqConfiguration.id
          changed = true
        }
        if (changed) {
          updated++
        }
      }
      if (updated > 0) {
        globalNexusConfiguration.save()
      }
    }
    LOGGER.debug(Messages.IqConfigNoIdToIdMigrator_MigratingGlobalConfigurationFinished(updated, total))
  }
}
