/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.util

import com.sonatype.nexus.ci.config.GlobalNexusConfiguration
import com.sonatype.nexus.ci.config.Nxrm2Configuration
import com.sonatype.nexus.ci.config.NxrmConfiguration

import hudson.util.FormValidation
import hudson.util.ListBoxModel
import org.apache.commons.lang.StringUtils
import org.apache.commons.validator.routines.UrlValidator

class NxrmUtil
{
  static boolean hasNexusRepositoryManagerConfiguration() {
    GlobalNexusConfiguration.all().get(GlobalNexusConfiguration.class).nxrmConfigs.size() > 0;
  }

  static FormValidation doCheckNexusInstanceId(final String value) {
    return FormUtil.validateNotEmpty(value, "Nexus Instance is required")
  }

  static ListBoxModel doFillNexusInstanceIdItems() {
    def globalConfiguration = GlobalNexusConfiguration.all().get(GlobalNexusConfiguration.class);
    return FormUtil.
        buildListBoxModel({ NxrmConfiguration it -> it.displayName }, { NxrmConfiguration it -> it.internalId },
            globalConfiguration.nxrmConfigs)
  }

  static FormValidation doCheckNexusRepositoryId(final String value) {
    return FormUtil.validateNotEmpty(value, "Nexus Repository is required")
  }

  static ListBoxModel doFillNexusRepositoryIdItems(final String nexusInstanceId) {
    if (StringUtils.isEmpty(nexusInstanceId)) {
      return FormUtil.buildListBoxModelWithEmptyOption()
    }
    def globalConfiguration = GlobalNexusConfiguration.all().get(GlobalNexusConfiguration.class);
    def configuration = globalConfiguration.nxrmConfigs.find { Nxrm2Configuration config ->
      config.internalId.equals(nexusInstanceId)
    }

    def client = RepositoryManagerClientUtil.buildRmClient(configuration.serverUrl, configuration.credentialsId)
    def repositories = client.getRepositoryList()
    return FormUtil.buildListBoxModel({ it.name }, { it.id }, repositories)
  }

  static boolean validUrl(final String input) {
    String[] schemes = ['http', 'https']
    UrlValidator validator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS)
    validator.isValid(input)
  }
}
