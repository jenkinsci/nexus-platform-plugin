/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import com.sonatype.nexus.api.iq.Action
import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation
import com.sonatype.nexus.api.iq.ComponentDisplayName
import com.sonatype.nexus.api.iq.ComponentDisplayNamePart
import com.sonatype.nexus.api.iq.ComponentFact
import com.sonatype.nexus.api.iq.ComponentIdentifier
import com.sonatype.nexus.api.iq.ConditionFact
import com.sonatype.nexus.api.iq.ConstraintFact
import com.sonatype.nexus.api.iq.PolicyAlert
import com.sonatype.nexus.api.iq.PolicyFact

import spock.lang.Specification

import static com.sonatype.nexus.ci.iq.TestDataGenerators.createAlert
import static java.util.Collections.singletonList

class PolicyFailureMessageFormatterTest
    extends Specification
{
  def failures
  def warnings
  def notifications
  def failingEvaluationResult
  def successfulEvaluationResult

  def setup() {
    failures = [createAlert(Action.ID_FAIL)]
    warnings = [createAlert(Action.ID_WARN), createAlert(Action.ID_WARN)]
    notifications = [createAlert(Action.ID_NOTIFY), createAlert(Action.ID_NOTIFY), createAlert(Action.ID_NOTIFY)]
    failingEvaluationResult = createFullModel(failures + warnings + notifications)
    successfulEvaluationResult = createFullModel([])
  }

  def 'failures and warnings are categorized correctly'() {
    when:
      def policyFailureMessageFormatter = new PolicyFailureMessageFormatter(failingEvaluationResult)

    then:
      policyFailureMessageFormatter.groupedActions.get(Action.ID_FAIL) == failures*.trigger
      policyFailureMessageFormatter.groupedActions.get(Action.ID_WARN) == warnings*.trigger
  }

  def 'notifications are ignored'() {
    when:
      def policyFailureMessageFormatter = new PolicyFailureMessageFormatter(failingEvaluationResult)

    then:
      ! policyFailureMessageFormatter.groupedActions.get(Action.ID_NOTIFY)
  }

  def 'has warnings only when warnings present'() {
    when:
      def failureFormatter = new PolicyFailureMessageFormatter(failingEvaluationResult)
      def successFormatter = new PolicyFailureMessageFormatter(successfulEvaluationResult)

    then:
      failureFormatter.hasWarnings()
      ! successFormatter.hasWarnings()
  }

  def 'has failures only when failures present'() {
    when:
      def failureFormatter = new PolicyFailureMessageFormatter(failingEvaluationResult)
      def successFormatter = new PolicyFailureMessageFormatter(successfulEvaluationResult)

    then:
      failureFormatter.hasFailures()
      ! successFormatter.hasFailures()
  }

  def 'creates failure report'() {
    when:
      def failureFormatter = new PolicyFailureMessageFormatter(failingEvaluationResult)

    then:
      failureFormatter.message == 'Nexus IQ reports policy failing due to \nPolicy(policyName) [\n Component' +
          '(displayName=value, hash=12hash34) [\n  Constraint(constraintName) [summary because: reason] ' +
          ']]\n\nNexus IQ reports policy warning due to \nPolicy(policyName) [\n Component(displayName=value,' +
          ' hash=12hash34) [\n  Constraint(constraintName) [summary because: reason] ]]\n\nNexus IQ reports ' +
          'policy warning due to \nPolicy(policyName) [\n Component(displayName=value, hash=12hash34) [\n  ' +
          'Constraint(constraintName) [summary because: reason] ]]\nThe detailed report can be viewed online at ' +
          'http://report/url\nSummary of policy violations: 2 critical, 3 severe, 5 moderate'
  }

  def 'creates success report'() {
    when:
      def successFormatter = new PolicyFailureMessageFormatter(successfulEvaluationResult)

    then:
      successFormatter.message == '\nThe detailed report can be viewed online at http://report/url\nSummary of ' +
          'policy violations: 2 critical, 3 severe, 5 moderate'
  }

  ApplicationPolicyEvaluation createFullModel(List<PolicyAlert> alerts) {
    return new ApplicationPolicyEvaluation(1, 2, 3, 5, alerts, false, 'http://report/url')
  }
}
