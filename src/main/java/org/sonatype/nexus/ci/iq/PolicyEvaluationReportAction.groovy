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

import hudson.model.Project
import hudson.model.Run
import jenkins.model.RunAction2

class PolicyEvaluationReportAction
  implements RunAction2
{
  private static final String CI_MAVEN_FORMAT = "maven"

  private static final String CI_A_NAME_FORMAT = "a-name"

  private static final String ICON_NAME = '/plugin/nexus-jenkins-plugin/images/24x24/nexus-iq.png'

  private static final String IQ_REPORT_NAME = 'iqreport'

  private static final String MENU_REPORT_TITLE = 'report will be here'

  private transient Run run

  private ApplicationPolicyEvaluation policyEvaluationResult

  private Project project

  PolicyEvaluationReportAction(Run run, final ApplicationPolicyEvaluation policyEvaluationResult) {
    this.run = run
    this.policyEvaluationResult = policyEvaluationResult
  }

  Run getRun() {
    return run;
  }

  int getBuildStepsCount() {
    return project.getBuilders().size()
  }

  int getPostBuildStepsCount() {
    return project.getPublishersList().size()
  }

  int getCount() {
    return this.policyEvaluationResult.getAffectedComponentCount()
  }

  String getUrl() {
    return this.policyEvaluationResult.applicationCompositionReportUrl
  }

  int getBuildNumber() {
    return run.number
  }

  String getColor(Integer policyLevel) {
    if (policyLevel > 7) {
      return 'red'
    }
    else if (policyLevel > 3) {
      return 'orange'
    }
    else if (policyLevel > 1) {
      return 'yellow'
    }
    else if (policyLevel == 1) {
      return 'blue'
    }

    return 'lightblue'
  }

  List<PolicyAlert> getAlerts() {
    return this.policyEvaluationResult.policyAlerts
  }

  Report getReport() {
    Report report = new Report()
    Map<String, ReportComponent> componentsMap = new HashMap<>()

    for (PolicyAlert alert : this.policyEvaluationResult.policyAlerts) {
      if (alert.actions?.size() == 0) {
        continue
      }

      ReportComponent component = new ReportComponent()
      component.policyName = alert.trigger.policyName
      component.policyLevel = alert.trigger.threatLevel
      component.constraints = new ArrayList<>()

      int failedCurrent = 0
      int warnCurrent = 0
      for (ComponentFact fact : alert.trigger.componentFacts) {
        if (fact.componentIdentifier) {
          component.componentName = getComponentName(fact);
        }

        for (ConstraintFact constraintFact : fact.constraintFacts) {
          Constraint constraint = new Constraint(constraintFact.constraintName,
              component.policyName, component.policyLevel, alert.actions[0]?.actionTypeId)
          if (constraint.action == 'fail') {
            failedCurrent++
          }
          if (constraint.action == 'warn') {
            warnCurrent++
          }

          for (ConditionFact conditionFact : constraintFact.conditionFacts) {
            constraint.conditions.add(new Condition(conditionFact.summary, conditionFact.reason))
          }
          component.constraints.add(constraint)
        }

      }
      if (failedCurrent != 0) {
        report.failedActionComponents++
      }
      if (warnCurrent != 0) {
        report.warnActionComponents++
      }
      report.failedActionViolations += failedCurrent
      report.warnActionViolations += warnCurrent
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

    report.components = componentsMap.values().sort { -it.policyLevel }
    return report
  }

  //private void addComponent(List<ReportComponent> components, ReportComponent component) {
  //  Integer elIndex = components?.findIndexOf { it.componentName == component.componentName}
  //
  //  if (elIndex == null || elIndex < 0) {
  //    components.add(component)
  //  } else {
  //    if (components.get(elIndex).policyLevel < component.policyLevel) {
  //      components.get(elIndex).policyLevel = component.policyLevel
  //    }
  //  }
  //}

  private static String getComponentName(ComponentFact fact) {
    if (fact.componentIdentifier.format == CI_MAVEN_FORMAT) {
      return "$fact.componentIdentifier.coordinates.groupId : $fact.componentIdentifier.coordinates.artifactId : " +
          "$fact.componentIdentifier.coordinates.version"
    }
    else if (fact.componentIdentifier.format == CI_A_NAME_FORMAT) {
      return fact.componentIdentifier.coordinates.name
    }

    return "Unknown Component with Unknown Format"
  }

  class Report
  {
    Integer failedActionComponents = 0

    Integer failedActionViolations = 0

    Integer warnActionComponents = 0

    Integer warnActionViolations = 0

    List<ReportComponent> components
  }

  class ReportComponent
  {
    String componentName

    String policyName

    Integer policyLevel

    List<Constraint> constraints
  }

  class Condition
  {
    String summary

    String reason

    Condition() {}

    Condition(final String summary, final String reason) {
      this.summary = summary
      this.reason = reason
    }
  }

  class Constraint
  {
    String name

    String policyName

    Integer policyLevel

    String action

    List<Condition> conditions

    Constraint() {}

    Constraint(final String name, final String policyName, final Integer policyLevel, final String action) {
      this.name = name
      this.policyName = policyName
      this.policyLevel = policyLevel
      this.action = action
      this.conditions = new ArrayList<>()
    }
  }

  @Override
  String getIconFileName() {
    return ICON_NAME
  }

  @Override
  String getDisplayName() {
    return MENU_REPORT_TITLE
  }

  @Override
  String getUrlName() {
    return IQ_REPORT_NAME
  }

  @Override
  void onAttached(final Run r) {
    this.run = run
  }

  @Override
  void onLoad(final Run r) {
    this.run = run;
  }
}
