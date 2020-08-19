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

class IqUtil
{

  static boolean hasNxiqConfiguration() {
    GlobalNexusConfiguration.globalNexusConfiguration.iqConfigs?.size() > 0
  }

  static NxiqConfiguration getNxiqConfiguration(final String iqServerId) {
    GlobalNexusConfiguration.globalNexusConfiguration.iqConfigs?.find {it.id == iqServerId }
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

  static ListBoxModel doFillIqStageItems(final String credentialsId, final Job job, final String iqServerId = null) {
    if (IqUtil.getNxiqConfiguration(iqServerId) != null) {
      def client = IqClientFactory.
          getIqClient(getClientFactoryConfiguration(credentialsId, job, iqServerId))
      FormUtil.newListBoxModel({ it.name }, { it.id }, client.getLicensedStages(Context.CI))
    }
    else {
      FormUtil.newListBoxModelWithEmptyOption()
    }
  }

  static ListBoxModel doFillIqApplicationItems(final String credentialsId,
                                               final Job job,
                                               final String iqServerId = null) {
    if (IqUtil.getNxiqConfiguration(iqServerId) != null) {
      def client = IqClientFactory.
          getIqClient(getClientFactoryConfiguration(credentialsId, job, iqServerId))
      FormUtil.newListBoxModel({ it.name }, { it.publicId }, client.getApplicationsForApplicationEvaluation())
    }
    else {
      FormUtil.newListBoxModelWithEmptyOption()
    }
  }

  static FormValidation verifyJobCredentials(final String jobCredentialsId,
                                             final ModelObject context,
                                             final String iqServerId = null) {
    NxiqConfiguration nxiqConfiguration = IqUtil.getNxiqConfiguration(iqServerId)
    return verifyJobCredentials(nxiqConfiguration.serverUrl, jobCredentialsId, context)
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

  static IqClientFactoryConfiguration getClientFactoryConfiguration(final String credentialsId, final Job job,
                                                                    final String iqServerId = null)
  {
    NxiqConfiguration nxiqConfiguration = IqUtil.getNxiqConfiguration(iqServerId)
    String credentialsId2 = credentialsId ?: nxiqConfiguration.credentialsId
    return new IqClientFactoryConfiguration(credentialsId: credentialsId2, serverUrl: URI.create(
        nxiqConfiguration.serverUrl), context: job)
  }
}
