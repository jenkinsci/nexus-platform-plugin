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

import javax.inject.Inject

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution
import org.jenkinsci.plugins.workflow.steps.StepContextParameter

@SuppressWarnings('UnnecessaryTransientModifier')
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
