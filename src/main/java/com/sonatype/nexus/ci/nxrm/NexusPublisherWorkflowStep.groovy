/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm

import com.sonatype.nexus.ci.util.NxrmUtil

import hudson.Extension
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

class NexusPublisherWorkflowStep
    extends AbstractStepImpl
    implements NexusPublisher
{
  String nexusInstanceId

  String nexusRepositoryId

  List<Package> packages

  @DataBoundConstructor
  NexusPublisherWorkflowStep(
      final String nexusInstanceId,
      final String nexusRepositoryId,
      final List<Package> packages)
  {
    this.nexusInstanceId = nexusInstanceId
    this.nexusRepositoryId = nexusRepositoryId
    this.packages = packages ?: []
  }

  @Extension
  static class DescriptorImpl
      extends AbstractStepDescriptorImpl
      implements NexusPublisherDescriptor
  {
    DescriptorImpl() {
      super(PackagePublisherExecution)
    }

    @Override
    String getFunctionName() {
      return 'nexusPublisher'
    }

    @Override
    String getDisplayName() {
      return 'Nexus Repository Manager Publisher'
    }

    FormValidation doCheckNexusInstanceId(@QueryParameter String value) {
      return NxrmUtil.doCheckNexusInstanceId(value)
    }

    ListBoxModel doFillNexusInstanceIdItems() {
      return NxrmUtil.doFillNexusInstanceIdItems()
    }

    FormValidation doCheckNexusRepositoryId(@QueryParameter String value) {
      return NxrmUtil.doCheckNexusRepositoryId(value)
    }

    ListBoxModel doFillNexusRepositoryIdItems(@QueryParameter String nexusInstanceId) {
      return NxrmUtil.doFillNexusRepositoryIdItems(nexusInstanceId)
    }
  }
}
