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

import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation

import hudson.model.Job
import hudson.model.Run
import org.kohsuke.stapler.StaplerResponse
import spock.lang.Specification

class PolicyEvaluationHealthActionTest
    extends Specification
{
  def 'it redirects to the application composition report'() {
    setup:
      def reportLink = 'http://localhost/reportLink'
      def policyEvaluation = new ApplicationPolicyEvaluation(0, 0, 0, 0, 0, 0, 0, 0, 0, [], reportLink)
      def healthAction = new PolicyEvaluationHealthAction('appId', 'stage', null, policyEvaluation)
      def response = Mock(StaplerResponse)

    when: 'browsing to index'
      healthAction.doIndex(null, response)

    then: 'response redirected to report'
      1 * response.sendRedirect(reportLink)
  }

  def 'it returns no project actions before build'() {
    setup:
      def healthAction = new PolicyEvaluationHealthAction('appId', 'stage', null, Mock(ApplicationPolicyEvaluation))

    when: 'getting project actions'
      def projectActions = healthAction.getProjectActions()

    then:
      projectActions.size() == 0
  }

  def 'it returns policy project action of builds parents'() {
    setup:
      def run = Mock(Run)
      def job = Mock(Job)
      run.getParent() >> job
      def healthAction = new PolicyEvaluationHealthAction('appId', 'stage', run, Mock(ApplicationPolicyEvaluation))

    when: 'getting project actions'
      def projectActions = healthAction.getProjectActions()

    then:
      projectActions.size() == 1
      PolicyEvaluationProjectAction projectAction = projectActions[0]
      projectAction.getJob() == job
  }

  def 'it returns build number'() {
    setup:
      def run = Mock(Run)
      run.getNumber() >> 3
      def healthAction = new PolicyEvaluationHealthAction('appId', 'stage', run, Mock(ApplicationPolicyEvaluation))

    when: 'getting build number'
      def buildNumber = healthAction.getBuildNumber()

    then:
      buildNumber == 3
  }
  
  def 'it returns the correct component and grandfathered policy violation counts'() {
    setup:
      def reportLink = 'http://localhost/reportLink'
      def policyEvaluation = new ApplicationPolicyEvaluation(1, 2, 3, 4, 11, 12, 13, 5, 1, [], reportLink)
      def healthAction = new PolicyEvaluationHealthAction('my-iq-app', 'build', null, policyEvaluation)
      def response = Mock(StaplerResponse)

    when: 'getting component and grandfathered policy violation counts'
      def affectedComponentCount = healthAction.affectedComponentCount
      def criticalComponentCount = healthAction.criticalComponentCount
      def severeComponentCount = healthAction.severeComponentCount
      def moderateComponentCount = healthAction.moderateComponentCount
      def criticalPolicyViolationCount = healthAction.criticalPolicyViolationCount
      def severePolicyViolationCount = healthAction.severePolicyViolationCount
      def moderatePolicyViolationCount = healthAction.moderatePolicyViolationCount
      def grandfatheredPolicyViolationCount = healthAction.grandfatheredPolicyViolationCount
      def totalPolicyViolationCount = healthAction.totalPolicyViolationCount
      def urlName = healthAction.urlName
      def appId = healthAction.applicationId
      def iqStage = healthAction.iqStage

    then:
      affectedComponentCount == 1
      criticalComponentCount == 2
      severeComponentCount == 3
      moderateComponentCount == 4
      criticalPolicyViolationCount == 11
      severePolicyViolationCount == 12
      moderatePolicyViolationCount == 13
      totalPolicyViolationCount == 36
      grandfatheredPolicyViolationCount == 5
      urlName == reportLink
      appId == 'my-iq-app'
      iqStage == 'build'
  }

  def 'health action and project action have matching report links'() {
    setup:
      def reportLink = 'http://localhost/reportLink'
      def run = Mock(Run)
      def policyEvaluation = new ApplicationPolicyEvaluation(1, 2, 3, 4, 11, 12, 13, 5, 1, [], reportLink)
      def healthAction = new PolicyEvaluationHealthAction('appId', 'stage', run, policyEvaluation)

    when:
      PolicyEvaluationProjectAction projectAction = (PolicyEvaluationProjectAction)healthAction.getProjectActions()[0]

    then:
      healthAction.urlName == reportLink
      projectAction.reportLink == reportLink
  }
}
