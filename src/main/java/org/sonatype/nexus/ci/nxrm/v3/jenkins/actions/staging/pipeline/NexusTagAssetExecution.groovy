package org.sonatype.nexus.ci.nxrm.v3.jenkins.actions.staging.pipeline

import javax.annotation.Nonnull

import hudson.FilePath
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import org.jenkinsci.plugins.workflow.steps.StepContext
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution

import static org.sonatype.nexus.ci.util.NxrmUtil.tagAsset

class NexusTagAssetExecution extends SynchronousNonBlockingStepExecution<Void>
{
  NexusTagAssetStep step

  NexusTagAssetExecution(@Nonnull NexusTagAssetStep step, StepContext context) {
    super(context)
    this.step = step
  }

  @SuppressWarnings('CatchThrowable')
  @Override
  protected Void run() {
    def run = context.get(Run)
    def taskListener = context.get(TaskListener)
    def logger = taskListener.getLogger()
    def workspace = context.get(FilePath)

    try {
      tagAsset(step, workspace)
    }
    catch (Throwable e) {
      logger.println("Unable to tag asset in Nexus [${e.getMessage()}]")
      run.setResult(Result.FAILURE)
      throw e
    }
  }
}
