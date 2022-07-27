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

import javax.servlet.ServletException

import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation

import hudson.model.Action
import hudson.model.HealthReport
import hudson.model.HealthReportingAction
import hudson.model.Run
import jenkins.tasks.SimpleBuildStep.LastBuildAction
import org.kohsuke.stapler.StaplerRequest
import org.kohsuke.stapler.StaplerResponse

class PolicyEvaluationHealthAction
    implements HealthReportingAction, LastBuildAction
{
  private final Run run

  private final String reportLink

  private final int affectedComponentCount

  private final int criticalComponentCount

  private final int severeComponentCount

  private final int moderateComponentCount

  private final int criticalPolicyViolationCount

  private final int severePolicyViolationCount

  private final int moderatePolicyViolationCount

  private final int grandfatheredPolicyViolationCount

  private final int totalComponentCount

  private final iqDisplayName 

  private final String iqServerUrl

  private final String applicationId

  private final String iqStage

  @SuppressWarnings('ParameterCount')
  PolicyEvaluationHealthAction(final String iqDisplayName,
                               final String iqServerUrl,
                               final String applicationId,
                               final String iqStage,
                               final Run run,
                               final ApplicationPolicyEvaluation policyEvaluationResult)
  {
    this.iqDisplayName = iqDisplayName
    this.iqServerUrl = iqServerUrl
    this.applicationId = applicationId
    this.iqStage = iqStage
    this.run = run
    this.reportLink = policyEvaluationResult.applicationCompositionReportUrl
    this.affectedComponentCount = policyEvaluationResult.affectedComponentCount
    this.criticalComponentCount = policyEvaluationResult.criticalComponentCount
    this.severeComponentCount = policyEvaluationResult.severeComponentCount
    this.moderateComponentCount = policyEvaluationResult.moderateComponentCount
    this.criticalPolicyViolationCount = policyEvaluationResult.criticalPolicyViolationCount
    this.severePolicyViolationCount = policyEvaluationResult.severePolicyViolationCount
    this.moderatePolicyViolationCount = policyEvaluationResult.moderatePolicyViolationCount
    this.grandfatheredPolicyViolationCount = policyEvaluationResult.grandfatheredPolicyViolationCount
    this.totalComponentCount = policyEvaluationResult.totalComponentCount
  }

  int getBuildNumber() {
    return run.number
  }

  int getCriticalComponentCount() {
    return criticalComponentCount
  }

  int getSevereComponentCount() {
    return severeComponentCount
  }

  int getModerateComponentCount() {
    return moderateComponentCount
  }

  int getCriticalPolicyViolationCount() {
    return criticalPolicyViolationCount
  }

  int getSeverePolicyViolationCount() {
    return severePolicyViolationCount
  }

  int getModeratePolicyViolationCount() {
    return moderatePolicyViolationCount
  }

  int getTotalPolicyViolationCount() {
    return criticalPolicyViolationCount + severePolicyViolationCount + moderatePolicyViolationCount
  }

  int getAffectedComponentCount() {
    return affectedComponentCount
  }

  int getGrandfatheredPolicyViolationCount() {
    return grandfatheredPolicyViolationCount
  }

  int getTotalComponentCount() {
    return totalComponentCount
  }

  String getIqDisplayName() {
    return iqDisplayName
  }

  String getIqServerUrl() {
    return iqServerUrl
  }

  String getApplicationId() {
    return applicationId
  }

  String getIqStage() {
    return iqStage
  }

  @Override
  HealthReport getBuildHealth() {
    return null
  }

  @Override
  String getIconFileName() {
    return '/plugin/nexus-jenkins-plugin/images/24x24/nexus-iq.png'
  }

  @Override
  String getDisplayName() {
    return Messages.IqPolicyEvaluation_ReportName()
  }

  @Override
  String getUrlName() {
    return reportLink
  }

  @Override
  Collection<? extends Action> getProjectActions() {
    if (!run) {
      return Collections.emptyList()
    }

    def job = run.getParent()
    return Collections.singleton(new PolicyEvaluationProjectAction(job, reportLink))
  }

  @SuppressWarnings(value = ['UnusedMethodParameter', 'SynchronizedMethod'])
  synchronized void doIndex(final StaplerRequest req, final StaplerResponse rsp)
      throws IOException, ServletException
  {
    rsp.sendRedirect(reportLink)
  }
}
