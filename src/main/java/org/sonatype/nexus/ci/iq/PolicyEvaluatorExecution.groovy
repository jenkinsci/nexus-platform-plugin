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

import javax.annotation.Nonnull
import javax.inject.Inject

import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation

import hudson.EnvVars
import hudson.FilePath
import hudson.Launcher
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import org.jenkinsci.plugins.workflow.actions.WarningAction
import org.jenkinsci.plugins.workflow.graph.FlowNode
import org.jenkinsci.plugins.workflow.steps.StepContext
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution

@SuppressWarnings('UnnecessaryTransientModifier')
class PolicyEvaluatorExecution
    extends SynchronousNonBlockingStepExecution<ApplicationPolicyEvaluation>
{
  @Inject
  private transient IqPolicyEvaluatorWorkflowStep iqPolicyEvaluator

  @Inject
  protected PolicyEvaluatorExecution(@Nonnull final StepContext context)
  {
    super(context)
  }

  @Override
  @SuppressWarnings('ConfusingMethodName')
  protected ApplicationPolicyEvaluation run() {
    Run run = context.get(Run)
    TaskListener taskListener = context.get(TaskListener)
    FilePath workspace = context.get(FilePath)
    Launcher launcher = context.get(Launcher)
    EnvVars envVars = context.get(EnvVars)
    FlowNode node = context.get(FlowNode)

    ApplicationPolicyEvaluation evaluationResult = IqPolicyEvaluatorUtil.evaluatePolicy(
        iqPolicyEvaluator, run, workspace, launcher, taskListener, envVars
    )

    Result result = run.getResult()
    String applicationId = envVars.expand(iqPolicyEvaluator.getIqApplication()?.applicationId)

    if (evaluationResult != null && evaluationResult.hasWarnings()) {
      node.addOrReplaceAction(new WarningAction(Result.UNSTABLE)
          .withMessage(Messages.IqPolicyEvaluation_EvaluationWarning(applicationId))
      )
    }

    return evaluationResult
  }
}
