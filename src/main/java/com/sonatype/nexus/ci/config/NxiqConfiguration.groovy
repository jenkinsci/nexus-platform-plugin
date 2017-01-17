/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.config

import javax.annotation.Nullable

import com.sonatype.nexus.api.exception.IqClientException
import com.sonatype.nexus.ci.util.FormUtil
import com.sonatype.nexus.ci.util.IqUtil

import hudson.Extension
import hudson.model.Describable
import hudson.model.Descriptor
import hudson.util.FormValidation
import hudson.util.FormValidation.Kind
import hudson.util.ListBoxModel
import jenkins.model.GlobalConfiguration
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

class NxiqConfiguration
    implements Describable<NxiqConfiguration>
{
  String serverUrl

  String credentialsId

  @DataBoundConstructor
  NxiqConfiguration(final String serverUrl,
                    final String credentialsId)
  {
    this.serverUrl = serverUrl
    this.credentialsId = credentialsId
  }

  @Override
  Descriptor<NxiqConfiguration> getDescriptor() {
    return jenkins.model.Jenkins.getInstance().getDescriptorOrDie(this.getClass());
  }

  static @Nullable String getServerUrl() {
    getIqConfig()?.@serverUrl
  }

  static @Nullable String getCredentialsId() {
    getIqConfig()?.@credentialsId
  }

  static @Nullable NxiqConfiguration getIqConfig() {
    return GlobalConfiguration.all().get(GlobalNexusConfiguration.class).iqConfigs?.find { true }
  }

  @Extension
  static class DescriptorImpl
      extends Descriptor<NxiqConfiguration>
  {
    @Override
    String getDisplayName() {
      return 'Nexus IQ Server'
    }

    @SuppressWarnings('unused')
    FormValidation doCheckServerUrl(@QueryParameter String value) {
      def validation = FormUtil.validateUrl(value)
      if (validation.kind == Kind.OK) {
        validation = FormUtil.validateNotEmpty(value, 'Server Url is required')
      }
      return validation
    }

    @SuppressWarnings('unused')
    ListBoxModel doFillCredentialsIdItems(@QueryParameter String serverUrl) {
      return FormUtil.buildCredentialsItems(serverUrl)
    }

    @SuppressWarnings('unused')
    FormValidation doVerifyCredentials(
        @QueryParameter String serverUrl,
        @QueryParameter String credentialsId) throws IOException
    {
      try {
        def applications = IqUtil.getApplicableApplications(serverUrl, credentialsId)

        return FormValidation.
            ok("Nexus IQ Server connection succeeded (${applications.size()} applications)")
      }
      catch (IqClientException e) {
        return FormValidation.error(e, 'Nexus IQ Server connection failed');
      }
    }
  }
}
