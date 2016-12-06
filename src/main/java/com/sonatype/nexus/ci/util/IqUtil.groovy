/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.util

import com.sonatype.nexus.api.iq.ApplicationSummary

class IqUtil
{
  /**
   * Return Nexus IQ Server applications which are applicable for evaluation.
   */
  static List<ApplicationSummary> getApplicableApplications(final String serverUrl, final String credentialsId) {
    def client = IqServerClientUtil.buildIqClient(serverUrl, credentialsId)
    return client.getApplicationsForApplicationEvaluation()
  }
}
