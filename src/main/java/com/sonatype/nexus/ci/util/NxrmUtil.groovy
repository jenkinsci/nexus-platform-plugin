/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.util

import com.sonatype.nexus.api.repository.RepositoryInfo
import com.sonatype.nexus.ci.config.GlobalNexusConfiguration
import com.sonatype.nexus.ci.config.Nxrm2Configuration
import com.sonatype.nexus.ci.config.NxrmConfiguration

import hudson.util.FormValidation
import hudson.util.ListBoxModel

class NxrmUtil
{
  static boolean hasNexusRepositoryManagerConfiguration() {
    GlobalNexusConfiguration.globalNexusConfiguration.nxrmConfigs.size() > 0;
  }

  static FormValidation doCheckNexusInstanceId(final String value) {
    return FormUtil.validateNotEmpty(value, "Nexus Instance is required")
  }

  static ListBoxModel doFillNexusInstanceIdItems() {
    return FormUtil.
        buildListBoxModel({ NxrmConfiguration it -> it.displayName }, { NxrmConfiguration it -> it.id },
            GlobalNexusConfiguration.globalNexusConfiguration.nxrmConfigs)
  }

  static FormValidation doCheckNexusRepositoryId(final String value) {
    return FormUtil.validateNotEmpty(value, "Nexus Repository is required")
  }

  static ListBoxModel doFillNexusRepositoryIdItems(final String nexusInstanceId) {
    if (!nexusInstanceId) {
      return FormUtil.buildListBoxModelWithEmptyOption()
    }
    def repositories = getApplicableRepositories(nexusInstanceId)
    return FormUtil.buildListBoxModel({ it.name }, { it.id }, repositories)
  }

  /**
   * Return Nexus repositories which are applicable for package upload. These are maven2 hosted repositories.
   */
  static List<RepositoryInfo> getApplicableRepositories(final String nexusInstanceId) {
    def configuration = GlobalNexusConfiguration.globalNexusConfiguration.nxrmConfigs.find { Nxrm2Configuration config ->
      config.id == nexusInstanceId
    }
    return getApplicableRepositories(configuration.serverUrl, configuration.credentialsId);
  }

  /**
   * Return Nexus repositories which are applicable for package upload. These are maven2 hosted repositories.
   */
  static List<RepositoryInfo> getApplicableRepositories(final String serverUrl, final String credentialsId) {
    def client = RepositoryManagerClientUtil.buildRmClient(serverUrl, credentialsId)
    return client.getRepositoryList().findAll {
      'maven2'.equalsIgnoreCase(it.format) &&
          'hosted'.equalsIgnoreCase(it.repositoryType) &&
          'release'.equalsIgnoreCase(it.repositoryPolicy)
    }
  }
}
