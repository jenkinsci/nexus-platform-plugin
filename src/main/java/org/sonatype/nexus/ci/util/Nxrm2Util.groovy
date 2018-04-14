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

import com.sonatype.nexus.api.repository.v2.RepositoryInfo

import static org.sonatype.nexus.ci.config.GlobalNexusConfiguration.getGlobalNexusConfiguration
import static org.sonatype.nexus.ci.config.NxrmVersion.NEXUS_2
import static org.sonatype.nexus.ci.util.RepositoryManagerClientUtil.nexus2Client

class Nxrm2Util
{
  /**
   * Return Nexus repositories which are applicable for package upload. These are maven2 hosted repositories.
   */
  static List<RepositoryInfo> getApplicableRepositories(final String nexusInstanceId) {
    def configuration = globalNexusConfiguration.nxrmConfigs.find { it.id == nexusInstanceId }

    if (configuration.version != NEXUS_2) {
      throw new IllegalArgumentException('Specified Nexus Repository Manager instance is not a 2.x server')
    }

    return getApplicableRepositories(configuration.serverUrl, configuration.credentialsId)
  }

  /**
   * Return Nexus repositories which are applicable for package upload. These are maven2 hosted repositories.
   */
  static List<RepositoryInfo> getApplicableRepositories(final String serverUrl, final String credentialsId) {
    def client = nexus2Client(serverUrl, credentialsId)
    return client.getRepositoryList().findAll {
      'maven2'.equalsIgnoreCase(it.format) &&
          'hosted'.equalsIgnoreCase(it.repositoryType) &&
          'release'.equalsIgnoreCase(it.repositoryPolicy)
    }
  }
}
