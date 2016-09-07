/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm

import javax.annotation.Nonnull

import com.sonatype.nexus.ci.util.NxrmUtil

import hudson.Extension
import hudson.FilePath
import hudson.Launcher
import hudson.model.AbstractProject
import hudson.model.Run
import hudson.model.TaskListener
import hudson.tasks.BuildStepDescriptor
import hudson.tasks.Builder
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import jenkins.tasks.SimpleBuildStep
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

class NxrmPublisherBuildStep
    extends Builder
    implements NxrmPublisher, SimpleBuildStep
{
  String nexusInstanceId

  String nexusRepositoryId

  List<Package> packages

  @DataBoundConstructor
  NxrmPublisherBuildStep(final String nexusInstanceId, final String nexusRepositoryId, final List<Package> packages) {
    this.nexusInstanceId = nexusInstanceId
    this.nexusRepositoryId = nexusRepositoryId
    this.packages = packages ?: []
  }

  @Override
  void perform(@Nonnull final Run run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher,
               @Nonnull final TaskListener listener) throws InterruptedException, IOException
  {
    PackageUploaderUtil.uploadPackage(this, run, listener, workspace)
  }

  @Extension
  public static final class DescriptorImpl
      extends BuildStepDescriptor<Builder>
      implements NxrmPublisherDescriptor
  {
    @Override
    public String getDisplayName() {
      'Nexus Repository Manager Publisher'
    }

    @Override
    boolean isApplicable(final Class<? extends AbstractProject> jobType) {
      true
    }

    public FormValidation doCheckNexusInstanceId(@QueryParameter String value) {
      NxrmUtil.doCheckNexusInstanceId(value)
    }

    public ListBoxModel doFillNexusInstanceIdItems() {
      NxrmUtil.doFillNexusInstanceIdItems()
    }

    public FormValidation doCheckNexusRepositoryId(@QueryParameter String value) {
      NxrmUtil.doCheckNexusRepositoryId(value)
    }

    public ListBoxModel doFillNexusRepositoryIdItems(@QueryParameter String nexusInstanceId) {
      NxrmUtil.doFillNexusRepositoryIdItems(nexusInstanceId)
    }
  }
}
