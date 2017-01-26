/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.iq

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
  private final Run run;

  private final String reportLink

  private final int affectedComponentCount

  private final int criticalComponentCount

  private final int severeComponentCount

  private final int moderateComponentCount

  public PolicyEvaluationHealthAction(final Run run,
                                      final ApplicationPolicyEvaluation policyEvaluationResult)
  {
    this.run = run
    this.reportLink = policyEvaluationResult.applicationCompositionReportUrl
    this.affectedComponentCount = policyEvaluationResult.affectedComponentCount
    this.criticalComponentCount = policyEvaluationResult.criticalComponentCount
    this.severeComponentCount = policyEvaluationResult.severeComponentCount
    this.moderateComponentCount = policyEvaluationResult.moderateComponentCount
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
    return 'Application Composition Report'
  }

  @Override
  String getUrlName() {
    return 'nexus-iq-application-composition-report'
  }

  @Override
  Collection<? extends Action> getProjectActions() {
    if (!run) {
      return Collections.emptyList()
    }

    def job = run.getParent()
    return Collections.singleton(new PolicyEvaluationProjectAction(job))
  }

  public synchronized void doIndex(final StaplerRequest req, final StaplerResponse rsp)
      throws IOException, ServletException
  {
    rsp.sendRedirect(reportLink);
  }
}
