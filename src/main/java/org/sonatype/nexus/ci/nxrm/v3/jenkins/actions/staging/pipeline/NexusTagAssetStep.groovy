package org.sonatype.nexus.ci.nxrm.v3.jenkins.actions.staging.pipeline

import javax.annotation.Nonnull

import org.sonatype.nexus.ci.nxrm.v3.jenkins.actions.staging.NexusTagAsset
import org.sonatype.nexus.ci.util.NxrmUtil

import hudson.Extension
import hudson.FilePath
import hudson.model.TaskListener
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import org.jenkinsci.plugins.workflow.steps.Step
import org.jenkinsci.plugins.workflow.steps.StepContext
import org.jenkinsci.plugins.workflow.steps.StepDescriptor
import org.jenkinsci.plugins.workflow.steps.StepExecution
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

class NexusTagAssetStep
    extends Step
    implements NexusTagAsset
{
  String nexusInstanceId

  String tagName

  String includes

  @DataBoundConstructor
  NexusTagAssetStep(final String nexusInstanceId, final String tagName, final String includes) {
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

  @Override
  StepExecution start(final StepContext context) throws Exception {
    new NexusTagAssetExecution(this, context)
  }

  @Extension
  static class DescriptorImpl
      extends StepDescriptor
  {
    @Override
    String getFunctionName() {
      return 'tagAsset'
    }

    @Override
    @Nonnull
    String getDisplayName() {
      return 'Tag asset in Nexus Repository Manager'
    }

    @Override
    Set<? extends Class<?>> getRequiredContext() {
      [FilePath, TaskListener]
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
