/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm

import javax.inject.Inject

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution
import org.jenkinsci.plugins.workflow.steps.StepContextParameter

@SuppressWarnings('UnnecessaryTransientModifier') // TODO find out why this uses transient. used by jenkins framework ?
class PackagePublisherExecution
    extends AbstractSynchronousNonBlockingStepExecution<Void>
{
  @Inject
  private transient NexusPublisherWorkflowStep nxrmPublisher

  @StepContextParameter
  private transient TaskListener taskListener

  @StepContextParameter
  private transient Run run

  @StepContextParameter
  private transient FilePath filePath

  @Override
  @SuppressWarnings('ConfusingMethodName')
  protected Void run() throws Exception {
    PrintStream logger = taskListener.getLogger()
    logger.println("Attempting to upload ${filePath} to Nexus.")

    try {
      PackageUploaderUtil.uploadPackage(nxrmPublisher, run, taskListener, filePath)
    }
    catch (IOException | InterruptedException ex) {
      logger.println("Upload of ${filePath} failed.")
      throw ex
    }

    logger.println("Upload of ${filePath} succeeded.")
    return null
  }
}
