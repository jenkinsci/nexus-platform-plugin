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
import com.sonatype.nexus.api.iq.ComponentFact
import com.sonatype.nexus.api.iq.ConditionFact
import com.sonatype.nexus.api.iq.ConstraintFact
import com.sonatype.nexus.api.iq.PolicyAlert

class PolicyEvaluationReportUtil
{

  public static final String IQ_FORMAT_MAVEN = 'maven'
  public static final String IQ_FORMAT_NPM = 'npm'
  public static final String IQ_FORMAT_NUGET = "nuget"

  static Report parseApplicationPolicyEvaluation(ApplicationPolicyEvaluation policyEvaluationResult) {
    Report report = new Report()
    Map<String, ReportComponent> componentsMap = [:]

    for (PolicyAlert alert : policyEvaluationResult.policyAlerts) {
      if (alert.actions?.size() == 0) {
        continue
      }

      ReportComponent component = new ReportComponent(componentName: 'Component-Unknown',
          policyName: alert.trigger.policyName, policyLevel: alert.trigger.threatLevel)
      component.constraints = getConstraints(component, alert)

      addComponent(componentsMap, component)
    }

    report.components = componentsMap.values().sort { -it.policyLevel }
    calculateViolations(report)
    return report
  }

  private static void calculateViolations(Report report) {
    for (ReportComponent comp: report.components) {
      int failedCurrent = 0
      int warnCurrent = 0

      for (Constraint constraint: comp.constraints) {
        if (constraint.action == 'warn') {
          warnCurrent++
        } else {
          failedCurrent++
        }
      }

      if (failedCurrent > 0) {
        report.failedActionComponents++
      }

      if (warnCurrent > 0) {
        report.warnActionComponents++
      }

      report.warnActionViolations += warnCurrent
      report.failedActionViolations += failedCurrent
    }
  }

  private static List<Condition> getConditions(List<ConditionFact> facts) {
    List<Condition> conditions = []
    for (ConditionFact conditionFact: facts) {
      conditions.add(new Condition(summary: conditionFact.summary, reason: conditionFact.reason))
    }
    return conditions
  }

  private static List<Constraint> getConstraints(ReportComponent component, PolicyAlert alert) {
    List<Constraint> constraints = []
    for (ComponentFact fact : alert.trigger.componentFacts) {
      if (fact.componentIdentifier) {
        component.componentName = getComponentName(fact)
      }

      for (ConstraintFact constraintFact : fact.constraintFacts) {
        Constraint constraint = new Constraint(name: constraintFact.constraintName, policyName: component.policyName,
            policyLevel: component.policyLevel, action: alert.actions[0]?.actionTypeId)
        constraint.conditions = getConditions(constraintFact.getConditionFacts())
        constraints.add(constraint)
      }
    }
    return constraints
  }

  private static void addComponent(Map<String, ReportComponent> componentsMap, ReportComponent component) {
    ReportComponent comp = componentsMap.get(component.getComponentName())
    if (comp) {
      comp.getConstraints().addAll(component.getConstraints())
      if (comp.policyLevel < component.policyLevel) {
        comp.policyLevel = component.policyLevel
      }
    }
    else {
      componentsMap.put(component.getComponentName(), component)
    }
  }

  private static String getComponentName(ComponentFact fact) {
    if (fact.componentIdentifier.format == IQ_FORMAT_MAVEN) {
      return "${fact.componentIdentifier.coordinates.groupId} : ${fact.componentIdentifier.coordinates.artifactId} : " +
          fact.componentIdentifier.coordinates.version
    }
    else if (fact.componentIdentifier.format == IQ_FORMAT_NPM || fact.componentIdentifier.format == IQ_FORMAT_NUGET) {
      return "${fact.componentIdentifier.coordinates.packageId} ${fact.componentIdentifier.coordinates.version}"
    }
    else {
      return "${fact.componentIdentifier.coordinates.name} ${fact.componentIdentifier.coordinates.version}"
    }
  }

  static class Report
  {
    Integer failedActionComponents = 0
    Integer failedActionViolations = 0
    Integer warnActionComponents = 0
    Integer warnActionViolations = 0
    List<ReportComponent> components = []
  }

  static class ReportComponent
  {
    String componentName
    String policyName
    Integer policyLevel
    List<Constraint> constraints = []
  }

  static class Condition
  {
    String summary
    String reason
  }

  static class Constraint
  {
    String name
    String policyName
    Integer policyLevel
    String action
    List<Condition> conditions = []
  }
}
