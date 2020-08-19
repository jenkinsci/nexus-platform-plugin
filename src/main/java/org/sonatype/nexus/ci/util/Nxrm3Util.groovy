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

import com.sonatype.nexus.api.repository.v3.Repository

import hudson.util.ListBoxModel

import static com.sonatype.nexus.api.common.NexusStringUtils.isNotBlank
import static org.sonatype.nexus.ci.config.GlobalNexusConfiguration.getGlobalNexusConfiguration
import static org.sonatype.nexus.ci.config.NxrmVersion.NEXUS_3
import static org.sonatype.nexus.ci.util.FormUtil.newListBoxModel
import static org.sonatype.nexus.ci.util.FormUtil.newListBoxModelWithEmptyOption
import static org.sonatype.nexus.ci.util.RepositoryManagerClientUtil.nexus3Client

class Nxrm3Util
{
  private static final String INSTANCE_IS_NOT_NXRM3 = 'Specified Nexus Repository Manager instance is not a 3.x server'

  /**
   * Return Nexus repositories which are applicable for package upload. These are maven2 hosted repositories.
   */
  static List<Repository> getApplicableRepositories(final String nexusInstanceId) {
    def configuration = globalNexusConfiguration.nxrmConfigs.find { it.id == nexusInstanceId }

    if (configuration.version != NEXUS_3) {
      throw new IllegalArgumentException(INSTANCE_IS_NOT_NXRM3)
    }

    getApplicableRepositories(configuration.serverUrl, configuration.credentialsId, 'maven2')
  }

  /**
   * Return Nexus repositories which are applicable for package upload. These are hosted repositories matching
   * the provided format.
   */
  static List<Repository> getApplicableRepositories(final String serverUrl, final String credentialsId,
                                                    final String format = null) {
    nexus3Client(serverUrl, credentialsId).getRepositories()
        .findAll { ('hosted'.equalsIgnoreCase(it.type)) && (isNotBlank(format) ?
        format.equalsIgnoreCase(it.format) : true)}
  }

  /**
   * Return Nexus all hosted repositories regardless of format.
   */
  static ListBoxModel doFillNexusHostedRepositoryIdItems(final String nexusInstanceId) {
    if (!nexusInstanceId) {
      return newListBoxModelWithEmptyOption()
    }

    def configuration = globalNexusConfiguration.nxrmConfigs.find { it.id == nexusInstanceId }

    if (configuration.version != NEXUS_3) {
      throw new IllegalArgumentException(INSTANCE_IS_NOT_NXRM3)
    }
    newListBoxModel({ it.name }, { it.name },
        getApplicableRepositories(configuration.serverUrl, configuration.credentialsId))
  }
}
