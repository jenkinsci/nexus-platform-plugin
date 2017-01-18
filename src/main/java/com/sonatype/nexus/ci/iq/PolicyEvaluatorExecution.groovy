/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.iq

import javax.inject.Inject

import hudson.FilePath
import hudson.Launcher
import hudson.model.Run
import hudson.model.TaskListener
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution
import org.jenkinsci.plugins.workflow.steps.StepContextParameter

class PolicyEvaluatorExecution
    extends AbstractSynchronousNonBlockingStepExecution<Void>
{
  @Inject
  private transient IqPolicyEvaluatorWorkflowStep iqPolicyEvaluator

  @StepContextParameter
  private transient TaskListener taskListener

  @StepContextParameter
  private transient Run run

  @StepContextParameter
  private transient FilePath workspace

  @StepContextParameter
  private transient Launcher launcher

  @Override
  protected Void run() throws Exception {
    PrintStream logger = taskListener.getLogger()
    logger.println("Evaluating policy")

    iqPolicyEvaluator.evaluatePolicy(run, workspace, launcher, taskListener)
  }
}
