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
package org.sonatype.nexus.ci.nxrm

import javax.annotation.Nonnull

import org.sonatype.nexus.ci.util.NxrmUtil

import hudson.Extension
import hudson.FilePath
import hudson.Launcher
import hudson.model.AbstractProject
import hudson.model.Run
import hudson.model.TaskListener
import hudson.tasks.BuildStepDescriptor
import hudson.tasks.Builder
import hudson.util.FormValidation
import hudson.util.FormValidation.Kind
import hudson.util.ListBoxModel
import jenkins.tasks.SimpleBuildStep
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

import static hudson.model.Result.FAILURE
import static org.sonatype.nexus.ci.config.NexusVersion.NEXUS3
import static org.sonatype.nexus.ci.nxrm.ComponentUploaderFactory.getComponentUploader

class NexusPublisherBuildStep
    extends Builder
    implements NexusPublisher, SimpleBuildStep
{
  String nexusInstanceId

  String nexusRepositoryId

  List<Package> packages

  Boolean isNexus3 = null

  @DataBoundConstructor
  NexusPublisherBuildStep(final String nexusInstanceId, final String nexusRepositoryId, final List<Package> packages) {
    this.nexusInstanceId = nexusInstanceId
    this.nexusRepositoryId = nexusRepositoryId
    this.packages = packages ?: []

    if (nexusInstanceId?.trim()) {
      isNexus3 = NxrmUtil.getNexusConfiguration(nexusInstanceId).nexusVersion == NEXUS3
    }
  }

  @SuppressWarnings('CatchThrowable')
  @Override
  void perform(@Nonnull final Run run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher,
               @Nonnull final TaskListener listener) throws InterruptedException, IOException
  {
    try {
      getComponentUploader(nexusInstanceId, run, workspace, listener).uploadComponents(nexusRepositoryId, packages)
    }
    catch (Throwable e) {
      run.setResult(FAILURE)
      throw e
    }
  }

  @Extension
  static final class DescriptorImpl
      extends BuildStepDescriptor<Builder>
      implements NexusPublisherDescriptor
  {
    Boolean isNexus3 = null

    @Override
    String getDisplayName() {
      'Nexus Repository Manager Publisher'
    }

    @Override
    boolean isApplicable(final Class<? extends AbstractProject> jobType) {
      true
    }

    FormValidation doCheckNexusInstanceId(@QueryParameter String value) {
      def validate = NxrmUtil.doCheckNexusInstanceId(value)

      if (validate.kind == Kind.OK) {
        isNexus3 = NxrmUtil.getNexusConfiguration(value).nexusVersion == NEXUS3
      }

      return validate
    }

    ListBoxModel doFillNexusInstanceIdItems() {
      NxrmUtil.doFillNexusInstanceIdItems()
    }

    FormValidation doCheckNexusRepositoryId(@QueryParameter String value) {
      NxrmUtil.doCheckNexusRepositoryId(value)
    }

    ListBoxModel doFillNexusRepositoryIdItems(@QueryParameter String nexusInstanceId) {
      // TODO kris: change type based on version
      NxrmUtil.doFillNexusRepositoryIdItems(nexusInstanceId)
    }
  }
}
