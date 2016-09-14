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
  NexusPublisherWorkflowStep(final String nexusInstanceId, final String nexusRepositoryId, final List<Package> packages) {
    this.nexusInstanceId = nexusInstanceId
    this.nexusRepositoryId = nexusRepositoryId
    this.packages = packages ?: new ArrayList<>()
  }

  @Extension
  public static class DescriptorImpl
      extends AbstractStepDescriptorImpl
      implements NexusPublisherDescriptor
  {
    public DescriptorImpl() {
      super(PackagePublisherExecution.class)
    }

    @Override
    String getFunctionName() {
      return 'nexusPublisher'
    }

    @Override
    public String getDisplayName() {
      return 'Nexus Repository Manager Publisher'
    }

    public FormValidation doCheckNexusInstanceId(@QueryParameter String value) {
      return NxrmUtil.doCheckNexusInstanceId(value)
    }

    public ListBoxModel doFillNexusInstanceIdItems() {
      return NxrmUtil.doFillNexusInstanceIdItems()
    }

    public FormValidation doCheckNexusRepositoryId(@QueryParameter String value) {
      return NxrmUtil.doCheckNexusRepositoryId(value)
    }

    public ListBoxModel doFillNexusRepositoryIdItems(@QueryParameter String nexusInstanceId) {
      return NxrmUtil.doFillNexusRepositoryIdItems(nexusInstanceId)
    }
  }
}
