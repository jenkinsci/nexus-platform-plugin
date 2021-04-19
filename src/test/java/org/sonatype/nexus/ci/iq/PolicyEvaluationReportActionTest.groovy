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

import com.sonatype.nexus.api.iq.Action
import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation
import com.sonatype.nexus.api.iq.ComponentFact
import com.sonatype.nexus.api.iq.ConstraintFact
import com.sonatype.nexus.api.iq.PolicyAlert
import com.sonatype.nexus.api.iq.PolicyFact

import hudson.model.Run
import spock.lang.Specification

class PolicyEvaluationReportActionTest
    extends Specification
{
  def 'initialization check'() {
    setup:
      def reportAction = new PolicyEvaluationReportAction('appId', 'stage', Mock(Run),
          Mock(ApplicationPolicyEvaluation))
    expect:
      reportAction.applicationId == 'appId'
      reportAction.iqStage == 'stage'
  }

  def "it should return color name"() {
    setup:
      def reportAction = new PolicyEvaluationReportAction('appId', 'stage', Mock(Run),
          Mock(ApplicationPolicyEvaluation))
    expect:
      reportAction.getColor(22) == 'red'
      reportAction.getColor(10) == 'red'
      reportAction.getColor(8) == 'red'
      reportAction.getColor(7) == 'orange'
      reportAction.getColor(4) == 'orange'
      reportAction.getColor(2) == 'yellow'
      reportAction.getColor(1) == 'blue'
      reportAction.getColor(0) == 'lightblue'
  }

  def 'it returns build number'() {
    setup:
      def run = Mock(Run)
      run.getNumber() >> 3
      def reportAction = new PolicyEvaluationReportAction('appId', 'stage', run, Mock(ApplicationPolicyEvaluation))
    when: 'getting build number'
      def buildNumber = reportAction.getBuildNumber()
    then:
      buildNumber == 3
  }

  def "GetUrl"() {
    setup:
      def reportLink = 'http://localhost/reportLink'
      def policyEvaluation = new ApplicationPolicyEvaluation(1, 2, 3, 4, 11, 12, 13, 5, 1, [], reportLink)
      def reportAction = new PolicyEvaluationReportAction('my-iq-app', 'build', null, policyEvaluation)
    when: 'getting report URL'
      def url = reportAction.getUrl()
    then:
      url == reportLink
  }

  def "GetReport should return empty report object"() {
    setup:
      def reportLink = 'http://localhost/reportLink'
      def policyEvaluation = new ApplicationPolicyEvaluation(1, 2, 3, 4, 11, 12, 13, 5, 1, [], reportLink)
      def reportAction = new PolicyEvaluationReportAction('my-iq-app', 'build', null, policyEvaluation)

    when: 'getting report result from empty object'
      def report = reportAction.getReport()

    then:
      report.failedActionViolations == 0
      report.failedActionComponents == 0
      report.warnActionViolations == 0
      report.warnActionComponents == 0
      report.components.size() == 0
  }

  def "GetReport should return report results"() {
    setup:

      def constraintFact = new ConstraintFact('', '', '', [])
      def compFact = new ComponentFact(null, 'hash', [constraintFact], [''], null)
      def fact = new PolicyFact('testId', 'testName', 10, [compFact])
      def alertFail = new PolicyAlert(fact, [new Action('fail', '', '')])
      def alertWarn = new PolicyAlert(fact, [new Action('warn', '', '')])
      def alertWarn2 = new PolicyAlert(fact, [new Action('warn', '', '')])
      def alertNoAction = new PolicyAlert(fact, [])

      def policyEvaluation = new ApplicationPolicyEvaluation(1, 2, 3, 4, 11, 12, 13, 5, 1,
          [alertFail, alertWarn, alertWarn2, alertNoAction], 'link')
      def reportAction = new PolicyEvaluationReportAction('my-iq-app', 'build', null, policyEvaluation)

    when: 'getting report result'
      def report = reportAction.getReport()

    then:
      report.failedActionViolations == 1
      report.failedActionComponents == 1
      report.warnActionViolations == 2
      report.warnActionComponents == 1
      report.components.size() == 1
  }
}
