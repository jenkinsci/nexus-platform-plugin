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
    if (iqConfigs && iqConfigs.size == 1) {
      NxiqConfiguration nxiqConfiguration = globalNexusConfiguration.iqConfigs.get(0)
      if (nxiqConfiguration.internalId == null) {
        nxiqConfiguration.internalId = GlobalNexusConfiguration.generateRandomId()
        nxiqConfiguration.id = 'NexusIQServer'
        nxiqConfiguration.displayName = 'Nexus IQ Server'

        globalNexusConfiguration.save()

        LOGGER.info(Messages.IqConfigNoIdToIdMigrator_MigratingGlobalConfigurationFinished())
      }
    }
  }
}
