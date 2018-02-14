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

import com.sonatype.nexus.api.exception.RepositoryManagerException

import org.sonatype.nexus.ci.util.FormUtil
import org.sonatype.nexus.ci.util.NxrmUtil

import hudson.Extension
import hudson.util.FormValidation
import hudson.util.FormValidation.Kind
import hudson.util.ListBoxModel
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

class Nxrm2Configuration
    extends NxrmConfiguration
{
  @DataBoundConstructor
  Nxrm2Configuration(final String id, final String internalId, final String displayName, final String serverUrl,
                     final String credentialsId)
  {
    this.id = id
    this.internalId = internalId
    this.displayName = displayName
    this.serverUrl = serverUrl
    this.credentialsId = credentialsId
  }

  @Extension
  static class DescriptorImpl
      extends NxrmConfiguration.NxrmDescriptor
  {
    DescriptorImpl() {
      super(Nxrm2Configuration)
    }

    @Override
    String getDisplayName() {
      return 'Nexus Repository Manager 2.x Server'
    }

    FormValidation doCheckDisplayName(@QueryParameter String value, @QueryParameter String internalId) {
      def globalConfigurations = GlobalNexusConfiguration.globalNexusConfiguration
      for (NxrmConfiguration config : globalConfigurations.nxrmConfigs) {
        if (config.internalId != internalId && config.displayName == value) {
          return FormValidation.error('Display Name must be unique')
        }
      }
      return FormUtil.validateNotEmpty(value, 'Display Name is required')
    }

    FormValidation doCheckId(@QueryParameter String value, @QueryParameter String internalId) {
      def globalConfigurations = GlobalNexusConfiguration.globalNexusConfiguration
      for (NxrmConfiguration config : globalConfigurations.nxrmConfigs) {
        if (config.internalId != internalId && config.id == value) {
          return FormValidation.error('Server ID must be unique')
        }
      }
      def validation = FormUtil.validateNoWhitespace(value, 'Server ID must not contain whitespace')
      if (validation.kind == Kind.OK) {
        validation = FormUtil.validateNotEmpty(value, 'Server ID is required')
      }
      return validation
    }

    FormValidation doCheckServerUrl(@QueryParameter String value) {
      def validation = FormUtil.validateUrl(value)
      if (validation.kind == Kind.OK) {
        validation = FormUtil.validateNotEmpty(value, 'Server Url is required')
      }
      return validation
    }

    ListBoxModel doFillCredentialsIdItems(@QueryParameter String serverUrl, @QueryParameter String credentialsId) {
      return FormUtil.newCredentialsItemsListBoxModel(serverUrl, credentialsId, null)
    }

    FormValidation doVerifyCredentials(
        @QueryParameter String serverUrl,
        @QueryParameter String credentialsId) throws IOException
    {
      try {
        def repositories = NxrmUtil.getApplicableRepositories(serverUrl, credentialsId)

        return FormValidation.ok("Nexus Repository Manager 2.x connection succeeded (${repositories.size()} hosted " +
            'release Maven 2 repositories)')
      }
      catch (RepositoryManagerException e) {
        return FormValidation.error(e, 'Nexus Repository Manager 2.x connection failed')
      }
    }
  }
}
