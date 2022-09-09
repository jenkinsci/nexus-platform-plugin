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

import com.sonatype.nexus.api.exception.IqClientException
import com.sonatype.nexus.api.iq.ApplicationSummary
import com.sonatype.nexus.api.iq.Context

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.Messages
import org.sonatype.nexus.ci.config.NxiqConfiguration
import org.sonatype.nexus.ci.iq.IqClientFactory
import org.sonatype.nexus.ci.iq.IqClientFactoryConfiguration

import hudson.model.Job
import hudson.model.ModelObject
import hudson.util.FormValidation
import hudson.util.ListBoxModel

import static org.sonatype.nexus.ci.util.FormUtil.newListBoxModel
import static org.sonatype.nexus.ci.util.FormUtil.validateNotEmpty

class IqUtil
{
  static List<NxiqConfiguration> getIqConfigurations() {
    GlobalNexusConfiguration.globalNexusConfiguration?.iqConfigs
  }

  static boolean hasIqConfiguration() {
    getIqConfigurations()?.size() > 0
  }

  static NxiqConfiguration getIqConfiguration(String iqInstanceId) {
    getIqConfigurations()?.find { it.id == iqInstanceId }
  }

  static NxiqConfiguration getFirstIqConfiguration() {
    getIqConfigurations()?.find { true }
  }

  /**
   * Return Nexus IQ Server applications which are applicable for evaluation.
   */
  static List<ApplicationSummary> getApplicableApplications(final String serverUrl,
                                                            final String credentialsId,
                                                            final ModelObject context) {
    def client = IqClientFactory.getIqClient(
        new IqClientFactoryConfiguration(credentialsId: credentialsId, context: context,
            serverUrl: URI.create(serverUrl)))
    return client.getApplicationsForApplicationEvaluation()
  }

  static FormValidation doCheckIqInstanceId(final String value) {
    return validateNotEmpty(value, 'IQ Instance is required')
  }

  static ListBoxModel doFillIqInstanceIdItems() {
    return newListBoxModel({ NxiqConfiguration it -> it.displayName }, { NxiqConfiguration it -> it.id },
        GlobalNexusConfiguration.globalNexusConfiguration.iqConfigs)
  }

  static ListBoxModel doFillIqStageItems(final String serverUrl, final String credentialsId, final Job job) {
    if (serverUrl && credentialsId) {
      def client = IqClientFactory.
          getIqClient(new IqClientFactoryConfiguration(serverUrl: new URI(serverUrl), credentialsId: credentialsId,
              context: job))
      FormUtil.newListBoxModel({ it.name }, { it.id }, client.getLicensedStages(Context.CI))
    }
    else {
      FormUtil.newListBoxModelWithEmptyOption()
    }
  }

  static ListBoxModel doFillIqApplicationItems(final String serverUrl,
                                               final String credentialsId,
                                               final Job job,
                                               final String organizationId)
  {
    if (serverUrl && credentialsId) {
      def client = IqClientFactory.
          getIqClient(new IqClientFactoryConfiguration(serverUrl: new URI(serverUrl), credentialsId: credentialsId,
              context: job))

      def applications

      if (organizationId) {
        applications = client.getApplicationsForApplicationEvaluation(organizationId)
      }
      else {
        applications = client.getApplicationsForApplicationEvaluation()
      }

      FormUtil.newListBoxModel({ it.name }, { it.publicId }, applications)
    }
    else {
      FormUtil.newListBoxModelWithEmptyOption()
    }
  }

  static FormValidation verifyJobCredentials(final String serverUrl,
                                             final String jobCredentialsId,
                                             final ModelObject context) {
    try {
      def applications = getApplicableApplications(serverUrl, jobCredentialsId, context)

      return FormValidation.ok(Messages.NxiqConfiguration_ConnectionSucceeded(applications.size()))
    }
    catch (IqClientException e) {
      return FormValidation.error(e, Messages.NxiqConfiguration_ConnectionFailed())
    }
  }

  static ListBoxModel doFillIqOrganizationItems(final String serverUrl, final String credentialsId, final Job job) {
    if (serverUrl && credentialsId) {
      def client = IqClientFactory.
          getIqClient(new IqClientFactoryConfiguration(serverUrl: new URI(serverUrl), credentialsId: credentialsId,
              context: job))
      FormUtil.newListBoxModel({ it.name }, { it.id }, client.getOrganizationsForApplicationEvaluation())
    }
    else {
      FormUtil.newListBoxModelWithEmptyOption()
    }
  }
}
