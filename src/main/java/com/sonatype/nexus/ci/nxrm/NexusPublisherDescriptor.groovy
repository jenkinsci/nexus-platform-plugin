/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm

import hudson.util.FormValidation
import hudson.util.ListBoxModel

interface NexusPublisherDescriptor
{
  @SuppressWarnings('unused')
  FormValidation doCheckNexusInstanceId(final String value)

  @SuppressWarnings('unused')
  ListBoxModel doFillNexusInstanceIdItems()

  @SuppressWarnings('unused')
  FormValidation doCheckNexusRepositoryId(final String value)

  @SuppressWarnings('unused')
  ListBoxModel doFillNexusRepositoryIdItems(final String nexusInstanceId)
}
