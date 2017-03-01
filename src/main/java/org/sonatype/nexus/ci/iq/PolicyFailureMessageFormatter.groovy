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
import com.sonatype.nexus.api.iq.PolicyFact

class PolicyFailureMessageFormatter
{
  private final Map<String, List<PolicyFact>> groupedActions

  private final ApplicationPolicyEvaluation evaluation

  PolicyFailureMessageFormatter(ApplicationPolicyEvaluation evaluation) {
    this.evaluation = evaluation

    groupedActions = [Action.ID_FAIL, Action.ID_WARN]
        .collectEntries { [it, findFactsFor(it) ] }
  }

  List<PolicyFact> findFactsFor(final String actionTypeId) {
    evaluation.policyAlerts
        .findAll { it.actions.find { it.actionTypeId == actionTypeId } }
        .collect { it.trigger }
  }

  String getMessage() {
    def failures = groupedActions.get(Action.ID_FAIL).
        collect { Messages.PolicyFailureMessageFormatter_PolicyFailing(it) }
    def warnings = groupedActions.get(Action.ID_WARN).
        collect { Messages.PolicyFailureMessageFormatter_PolicyWarning(it) }
    def summary = [Messages.PolicyFailureMessageFormatter_EvaluationReport(evaluation.applicationCompositionReportUrl),
                   Messages.PolicyFailureMessageFormatter_EvaluationSummary(evaluation.criticalComponentCount,
                       evaluation.severeComponentCount, evaluation.moderateComponentCount)]
    return ([(failures + warnings).join('\n\n')] + summary).join('\n')
  }

  boolean hasWarnings() {
    groupedActions.get(Action.ID_WARN)
  }

  boolean hasFailures() {
    groupedActions.get(Action.ID_FAIL)
  }
}
