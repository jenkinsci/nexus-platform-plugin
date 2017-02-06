/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.util

import javax.annotation.Nullable

import com.sonatype.nexus.api.iq.ApplicationSummary
import com.sonatype.nexus.api.iq.Context
import com.sonatype.nexus.ci.config.NxiqConfiguration
import com.sonatype.nexus.ci.iq.IqClientFactory

import hudson.util.ListBoxModel

class IqUtil
{
  /**
   * Return Nexus IQ Server applications which are applicable for evaluation.
   */
  static List<ApplicationSummary> getApplicableApplications(final String serverUrl, final String credentialsId) {
    def client = IqClientFactory.getIqClient(new URI(serverUrl), credentialsId)
    return client.getApplicationsForApplicationEvaluation()
  }

  static ListBoxModel doFillIqStageItems(@Nullable final String credentialsId) {
    if (NxiqConfiguration.iqConfig) {
      def client = IqClientFactory.getIqClient(credentialsId)
      FormUtil.buildListBoxModel({ it.name }, { it.id }, client.getLicensedStages(Context.CI))
    } else {
      FormUtil.buildListBoxModelWithEmptyOption()
    }
  }

  static ListBoxModel doFillIqApplicationItems(@Nullable final String credentialsId) {
    if (NxiqConfiguration.iqConfig) {
      def client = IqClientFactory.getIqClient(credentialsId)
      FormUtil.buildListBoxModel({ it.name }, { it.publicId }, client.getApplicationsForApplicationEvaluation())
    } else {
      FormUtil.buildListBoxModelWithEmptyOption()
    }
  }
}
