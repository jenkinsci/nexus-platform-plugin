package org.sonatype.nexus.ci.nxrm.v3.pipeline

import javax.annotation.Nonnull

import com.sonatype.nexus.api.exception.RepositoryManagerException

import org.sonatype.nexus.ci.config.NxrmVersion
import org.sonatype.nexus.ci.util.FormUtil
import org.sonatype.nexus.ci.util.NxrmUtil
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import com.google.common.collect.ImmutableSet
import hudson.Extension
import hudson.model.Run
import hudson.model.TaskListener
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import org.jenkinsci.plugins.workflow.steps.Step
import org.jenkinsci.plugins.workflow.steps.StepContext
import org.jenkinsci.plugins.workflow.steps.StepDescriptor
import org.jenkinsci.plugins.workflow.steps.StepExecution
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

import static hudson.model.Result.FAILURE
import static org.sonatype.nexus.ci.nxrm.Messages.DeleteComponentsBuildStep_DisplayName
import static org.sonatype.nexus.ci.nxrm.Messages.DeleteComponentsBuildStep_FunctionName
import static org.sonatype.nexus.ci.nxrm.Messages.DeleteComponentsBuildStep_Validation_TagNameRequired

class DeleteComponentsStep
    extends Step
{
  final String nexusInstanceId

  final String tagName

  @DataBoundConstructor
  DeleteComponentsStep(final String nexusInstanceId, final String tagName) {
    this.nexusInstanceId = nexusInstanceId
    this.tagName = tagName
  }

  @Override
  StepExecution start(final StepContext context) throws Exception {
    new DeleteComponentsStepExecution(nexusInstanceId, tagName, context)
  }

  @Extension
  static final class DescriptorImpl
      extends StepDescriptor
  {
    @Override
    Set<? extends Class<?>> getRequiredContext() {
      ImmutableSet.of(Run.class, TaskListener.class)
    }

    @Override
    String getFunctionName() {
      DeleteComponentsBuildStep_FunctionName()
    }

    @Override
    String getDisplayName() {
      DeleteComponentsBuildStep_DisplayName()
    }

    FormValidation doCheckNexusInstanceId(@QueryParameter String value) {
      NxrmUtil.doCheckNexusInstanceId(value)
    }

    ListBoxModel doFillNexusInstanceIdItems() {
      NxrmUtil.doFillNexusInstanceIdItems(NxrmVersion.NEXUS_3)
    }

    FormValidation doCheckTagName(@QueryParameter String tagName) {
      FormUtil.validateNotEmpty(tagName, DeleteComponentsBuildStep_Validation_TagNameRequired())
    }
  }

  static class DeleteComponentsStepExecution
      extends StepExecution
  {
    private final String nexusInstanceId

    private final String tagName

    DeleteComponentsStepExecution(final String nexusInstanceId, final String tagName, final StepContext context) {
      super(context)
      this.tagName = tagName
      this.nexusInstanceId = nexusInstanceId
    }

    @Override
    boolean start() throws Exception {
      def logger = context.get(TaskListener.class).getLogger()
      def run = context.get(Run.class)

      try {
        def client = RepositoryManagerClientUtil.nexus3Client(nexusInstanceId)
        context.onSuccess(client.delete(tagName))
        return true
      }
      catch (RepositoryManagerException e) {
        logger.println("Failing build due to: ${e.responseMessage.orElse(e.message)}")
        run.setResult(FAILURE)
        context.onFailure(e)
        return false
      }
    }

    @Override
    void stop(@Nonnull final Throwable cause) throws Exception {
      // noop (synchronous step)
    }
  }
}
