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
package org.sonatype.nexus.ci.iq

import javax.inject.Inject

import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation

import hudson.FilePath
import hudson.Launcher
import hudson.model.Run
import hudson.model.TaskListener
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution
import org.jenkinsci.plugins.workflow.steps.StepContextParameter

@SuppressWarnings('UnnecessaryTransientModifier')
class PolicyEvaluatorExecution
    extends AbstractSynchronousNonBlockingStepExecution<ApplicationPolicyEvaluation>
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
  @SuppressWarnings('ConfusingMethodName')
  protected ApplicationPolicyEvaluation run() throws Exception {
    iqPolicyEvaluator.evaluatePolicy(run, workspace, launcher, taskListener)
  }
}
