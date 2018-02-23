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

import org.sonatype.nexus.ci.config.Nxrm2Configuration

import hudson.util.FormValidation
import hudson.util.ListBoxModel

import static org.sonatype.nexus.ci.config.GlobalNexusConfiguration.globalNexusConfiguration

class NxrmUtil
{
  static boolean hasNexusRepositoryManagerConfiguration() {
    globalNexusConfiguration.nxrmConfigs.size() > 0
  }

  static Nxrm2Configuration getNexusConfiguration(final String nexusInstanceId) {
    globalNexusConfiguration.nxrmConfigs.find { return it.id == nexusInstanceId }
  }

  static FormValidation doCheckNexusInstanceId(final String value) {
    return FormUtil.validateNotEmpty(value, 'Nexus Instance is required')
  }

  static ListBoxModel doFillNexusInstanceIdItems() {
    return FormUtil.
        newListBoxModel({ Nxrm2Configuration it -> it.displayName }, { Nxrm2Configuration it -> it.id },
            globalNexusConfiguration.nxrmConfigs)
  }

  static FormValidation doCheckNexusRepositoryId(final String value) {
    return FormUtil.validateNotEmpty(value, 'Nexus Repository is required')
  }

  static ListBoxModel doFillNexusRepositoryIdItems(final String nexusInstanceId) {
    if (!nexusInstanceId) {
      return FormUtil.newListBoxModelWithEmptyOption()
    }
    def repositories = getApplicableRepositories(nexusInstanceId)
    return FormUtil.newListBoxModel({ it.name }, { it.id }, repositories)
  }

  /**
   * Return Nexus repositories which are applicable for package upload. These are maven2 hosted repositories.
   */
  static List<RepositoryInfo> getApplicableRepositories(final String nexusInstanceId) {
    def configuration = globalNexusConfiguration.nxrmConfigs.find {
      Nxrm2Configuration config -> config.id == nexusInstanceId
    }
    return getApplicableRepositories(configuration.serverUrl, configuration.credentialsId)
  }

  /**
   * Return Nexus repositories which are applicable for package upload. These are maven2 hosted repositories.
   */
  static List<RepositoryInfo> getApplicableRepositories(final String serverUrl, final String credentialsId) {
    def client = RepositoryManagerClientUtil.newRepositoryManagerClient(serverUrl, credentialsId)
    return client.getRepositoryList().findAll {
      'maven2'.equalsIgnoreCase(it.format) &&
          'hosted'.equalsIgnoreCase(it.repositoryType) &&
          'release'.equalsIgnoreCase(it.repositoryPolicy)
    }
  }
}
