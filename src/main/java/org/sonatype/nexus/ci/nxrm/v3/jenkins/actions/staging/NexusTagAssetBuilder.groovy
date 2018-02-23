package org.sonatype.nexus.ci.nxrm.v3.jenkins.actions.staging

import javax.annotation.Nonnull

import org.sonatype.nexus.ci.util.NxrmUtil

import hudson.Extension
import hudson.FilePath
import hudson.Launcher
import hudson.model.AbstractProject
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import hudson.tasks.BuildStepDescriptor
import hudson.tasks.Builder
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import jenkins.tasks.SimpleBuildStep
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

class NexusTagAssetBuilder
    extends Builder
    implements SimpleBuildStep, NexusTagAsset
{
  String nexusInstanceId

  String tagName

  String includes

  @DataBoundConstructor
  NexusTagAssetBuilder(final String nexusInstanceId, final String tagName, final String includes) {
    this.nexusInstanceId = nexusInstanceId
    this.tagName = tagName
    this.includes = includes
  }

  String getNexusInstanceId() {
    return nexusInstanceId
  }

  String getTagName() {
    return tagName
  }

  String getIncludes() {
    return includes
  }

  @SuppressWarnings('CatchThrowable')
  @Override
  void perform(@Nonnull final Run run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher,
               @Nonnull final TaskListener listener) throws InterruptedException, IOException
  {
    def logger = listener.getLogger()

    try {
      NxrmUtil.tagAsset(this, workspace)
    }
    catch (Throwable e) {
      logger.println("Unable to tag asset in Nexus [${e.getMessage()}]")
      run.setResult(Result.FAILURE)
      throw e
    }
  }

  @Extension
  static final class DescriptorImpl
      extends BuildStepDescriptor<Builder>
  {
    @Override
    String getDisplayName() {
      'Tag Asset (Nexus Repository Manager)'
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
      if (value == null || value.isAllWhitespace()) {
        return FormValidation.error('Tag cannot be empty')
      }

      return FormValidation.ok()
    }
  }
}
