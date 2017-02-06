/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import hudson.util.FormValidation
import hudson.util.ListBoxModel

interface IqPolicyEvaluatorDescriptor
{
  FormValidation doCheckIqStage(String value)

  ListBoxModel doFillIqStageItems(String jobCredentialsId)

  FormValidation doCheckIqApplication(String value)

  ListBoxModel doFillIqApplicationItems(String jobCredentialsId)

  FormValidation doCheckScanPattern(String scanPattern)

  FormValidation doCheckFailBuildOnNetworkError(String value)

  ListBoxModel doFillJobCredentialsIdItems()
}
