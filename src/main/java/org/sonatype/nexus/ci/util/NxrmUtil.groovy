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

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.NxrmConfiguration
import org.sonatype.nexus.ci.config.NxrmVersion

import hudson.util.FormValidation
import hudson.util.ListBoxModel

import static org.sonatype.nexus.ci.config.NxrmVersion.NEXUS_2
import static org.sonatype.nexus.ci.config.NxrmVersion.NEXUS_3
import static org.sonatype.nexus.ci.util.FormUtil.newListBoxModel
import static org.sonatype.nexus.ci.util.FormUtil.newListBoxModelWithEmptyOption
import static org.sonatype.nexus.ci.util.FormUtil.validateNotEmpty

class NxrmUtil
{
  static boolean hasNexusRepositoryManagerConfiguration() {
    GlobalNexusConfiguration.globalNexusConfiguration.nxrmConfigs?.size() > 0
  }

  static NxrmConfiguration getNexusConfiguration(final String nexusInstanceId) {
    GlobalNexusConfiguration.globalNexusConfiguration.nxrmConfigs.find { return it.id == nexusInstanceId }
  }

  static FormValidation doCheckNexusInstanceId(final String value) {
    return validateNotEmpty(value, 'Nexus Instance is required')
  }

  static ListBoxModel doFillNexusInstanceIdItems() {
    return newListBoxModel({ NxrmConfiguration it -> it.displayName }, { NxrmConfiguration it -> it.id },
        GlobalNexusConfiguration.globalNexusConfiguration.nxrmConfigs)
  }

  static ListBoxModel doFillNexusInstanceIdItems(NxrmVersion version) {
    newListBoxModel({ it.displayName }, { it.id },
        GlobalNexusConfiguration.globalNexusConfiguration.nxrmConfigs.findAll({ it.version == version }))
  }

  static FormValidation doCheckNexusRepositoryId(final String value) {
    return validateNotEmpty(value, 'Nexus Repository is required')
  }

  static ListBoxModel doFillNexusRepositoryIdItems(final String nexusInstanceId) {
    if (!nexusInstanceId) {
      return newListBoxModelWithEmptyOption()
    }

    def configuration = getNexusConfiguration(nexusInstanceId)

    switch (configuration.version) {
      case NEXUS_2:
        return newListBoxModel({ it.name }, { it.id },
            Nxrm2Util.getApplicableRepositories(configuration.serverUrl, configuration.credentialsId))
      case NEXUS_3:
        return newListBoxModel({ it.name }, { it.name },
            Nxrm3Util.getApplicableRepositories(configuration.serverUrl, configuration.credentialsId, 'maven2'))
    }
  }

  static boolean isVersion(final String nexusInstanceId, final NxrmVersion version) {
    getNexusConfiguration(nexusInstanceId)?.version == version
  }
}
