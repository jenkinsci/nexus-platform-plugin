package org.sonatype.nexus.ci.nxrm.v3.jenkins.actions.staging

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
import hudson.util.ListBoxModel
import jenkins.tasks.SimpleBuildStep
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

class NexusTagComponentBuilder
    extends Builder
    implements SimpleBuildStep
{
  String nexusInstanceId

  String tagName

  String group

  String name

  String version

  @DataBoundConstructor
  NexusTagComponentBuilder(final String nexusInstanceId, final String tagName, final String group, final String name,
                           final String version)
  {
    this.nexusInstanceId = nexusInstanceId
    this.tagName = tagName
    this.group = group
    this.name = name
    this.version = version
  }

  @SuppressWarnings('CatchThrowable')
  @Override
  void perform(@Nonnull final Run run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher,
               @Nonnull final TaskListener listener) throws InterruptedException, IOException
  {
    // TODO kris: tag component
  }

  @Extension
  static final class DescriptorImpl
      extends BuildStepDescriptor<Builder>
  {
    @Override
    String getDisplayName() {
      'Tag Component (Nexus Repository Manager)'
    }

    @Override
    boolean isApplicable(final Class<? extends AbstractProject> jobType) {
      true
    }

    FormValidation doCheckNexusInstanceId(@QueryParameter String value) {
      NxrmUtil.doCheckNexusInstanceId(value)
    }

    ListBoxModel doFillNexusInstanceIdItems() {
      NxrmUtil.doFillNexusInstanceIdItems()
    }

    FormValidation doCheckTagName(@QueryParameter String value) {
      validateNonEmpty('Tag', value)
    }

    FormValidation doCheckName(@QueryParameter String value) {
      validateNonEmpty('Name', value)
    }

    FormValidation doCheckVersion(@QueryParameter String value) {
      validateNonEmpty('Version', value)
    }

    private FormValidation validateNonEmpty(String field, String value) {
      (value == null || value.isAllWhitespace()) ? FormValidation.
          error("${field} cannot be empty") : FormValidation.ok()
    }
  }
}

