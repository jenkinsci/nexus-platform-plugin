package org.sonatype.nexus.ci.nxrm.v3

import javax.annotation.Nonnull

import com.sonatype.nexus.api.exception.RepositoryManagerException

import org.sonatype.nexus.ci.config.NxrmVersion
import org.sonatype.nexus.ci.util.FormUtil
import org.sonatype.nexus.ci.util.NxrmUtil
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

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

import static hudson.model.Result.FAILURE
import static org.sonatype.nexus.ci.nxrm.Messages.DeleteComponentsBuildStep_DisplayName
import static org.sonatype.nexus.ci.nxrm.Messages.DeleteComponentsBuildStep_Validation_TagNameRequired

class DeleteComponentsBuildStep
    extends Builder
    implements SimpleBuildStep
{
  final String nexusInstanceId

  final String tagName

  @DataBoundConstructor
  DeleteComponentsBuildStep(final String nexusInstanceId, final String tagName) {
    this.nexusInstanceId = nexusInstanceId
    this.tagName = tagName
  }

  @Override
  void perform(@Nonnull final Run run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher,
               @Nonnull final TaskListener listener) throws InterruptedException, IOException
  {
    try {
      def client = RepositoryManagerClientUtil.nexus3Client(nexusInstanceId)
      client.delete(tagName)
    }
    catch (RepositoryManagerException e) {
      listener.getLogger().println("Failing build due to: ${e.responseMessage.orElse(e.message)}")
      run.setResult(FAILURE)
    }
  }

  @Extension
  static final class DescriptorImpl
      extends BuildStepDescriptor<Builder>
  {
    @Override
    String getDisplayName() {
      DeleteComponentsBuildStep_DisplayName()
    }

    @Override
    boolean isApplicable(final Class<? extends AbstractProject> jobType) {
      true
    }

    FormValidation doCheckTagName(@QueryParameter String tagName) {
      FormUtil.validateNotEmpty(tagName, DeleteComponentsBuildStep_Validation_TagNameRequired())
    }

    FormValidation doCheckNexusInstanceId(@QueryParameter String value) {
      NxrmUtil.doCheckNexusInstanceId(value)
    }

    ListBoxModel doFillNexusInstanceIdItems() {
      NxrmUtil.doFillNexusInstanceIdItems(NxrmVersion.NEXUS_3)
    }
  }
}
