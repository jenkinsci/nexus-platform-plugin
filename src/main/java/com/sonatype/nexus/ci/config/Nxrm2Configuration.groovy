/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.config

import com.sonatype.nexus.api.ApiStub.NexusClientFactory
import com.sonatype.nexus.ci.util.FormUtil

import hudson.Extension
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

class Nxrm2Configuration
    extends NxrmConfiguration
{
  @DataBoundConstructor
  Nxrm2Configuration(final String internalId, final String displayName, final String serverUrl,
                     final String credentialsId)
  {
    this.internalId = internalId
    this.displayName = displayName
    this.serverUrl = serverUrl
    this.credentialsId = credentialsId
  }

  @Extension
  public static class DescriptorImpl
      extends NxrmConfiguration.NxrmDescriptor
  {
    public DescriptorImpl() {
      super(Nxrm2Configuration.class)
    }

    @Override
    public String getDisplayName() {
      return 'Nexus Repository Manager 2.x Server'
    }

    @SuppressWarnings('unused')
    public FormValidation doCheckDisplayName(@QueryParameter String value, @QueryParameter String internalId) {
      def globalConfigurations = GlobalNexusConfiguration.all().get(GlobalNexusConfiguration.class)
      for (NxrmConfiguration config : globalConfigurations.nxrmConfigs) {
        if (!config.internalId.equals(internalId) && config.displayName.equals(value)) {
          return FormValidation.error('Display names must be unique')
        }
      }
      return FormValidation.ok()
    }

    @SuppressWarnings('unused')
    public FormValidation doCheckServerUrl(@QueryParameter String value) {
      return FormUtil.validateUrl(value)
    }

    @SuppressWarnings('unused')
    public ListBoxModel doFillCredentialsIdItems(@QueryParameter String serverUrl) {
      return FormUtil.buildCredentialsItems(serverUrl)
    }

    @SuppressWarnings('unused')
    public FormValidation doVerifyCredentials(
        @QueryParameter String serverUrl,
        @QueryParameter String credentialsId) throws IOException
    {
      try {
        // TODO: Validate NXRM credentials
        def client = NexusClientFactory.buildRmClient(serverUrl, credentialsId)
        def repositories = client.getNxrmRepositories()

        return FormValidation.
            ok("Nexus Repository Manager 2.x connection succeeded (${repositories.size()} repositories)")
      }
      catch (IOException e) {
        return FormValidation.error(e, 'Nexus Repository Manager 2.x connection failed');
      }
    }
  }
}
