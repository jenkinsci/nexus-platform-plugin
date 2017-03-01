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
      def policyEvaluation = new ApplicationPolicyEvaluation(0, 0, 0, 0, [], false, reportLink)
      def healthAction = new PolicyEvaluationHealthAction(null, policyEvaluation)
      def response = Mock(StaplerResponse)

    when: 'browsing to index'
      healthAction.doIndex(null, response)

    then: 'response redirected to report'
      1 * response.sendRedirect(reportLink)
  }

  def 'it returns no project actions before build'() {
    setup:
      def healthAction = new PolicyEvaluationHealthAction(null, Mock(ApplicationPolicyEvaluation))

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
      def healthAction = new PolicyEvaluationHealthAction(run, Mock(ApplicationPolicyEvaluation))

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
      def healthAction = new PolicyEvaluationHealthAction(run, Mock(ApplicationPolicyEvaluation))

    when: 'getting build number'
      def buildNumber = healthAction.getBuildNumber()

    then:
      buildNumber == 3
  }
}
