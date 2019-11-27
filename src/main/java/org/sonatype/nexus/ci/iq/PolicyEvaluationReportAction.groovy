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

  private static final String MENU_REPORT_TITLE = 'Nexus IQ Build Failure Report'

  private static final String IQ_SPACE_SHIP_PNG = '/plugin/nexus-jenkins-plugin/images/sonatype-iq-rocketship.png';
  private static final String IQ_SPACE_SHIP_SUCCESS_MESSAGE = 'We\'re all clear!';
  private static final String SPACE_SHIP_ALT = 'A Space Ship';

  private static final String IQ_BOAT_PNG = '/plugin/nexus-jenkins-plugin/images/sonatype-iq-boat.png';
  private static final String IQ_BOAT_SUCCESS_MESSAGE = 'We\'re smooth sailing!';
  private static final String BOAT_ALT = 'A Boat';


  private transient Run run

  private ApplicationPolicyEvaluation policyEvaluationResult

  private Project project

  private final String applicationId

  private final String iqStage

  PolicyEvaluationReportAction(final String applicationId, final String iqStage, final Run run,
                               final ApplicationPolicyEvaluation policyEvaluationResult) {
    this.applicationId = applicationId
    this.iqStage = iqStage
    this.run = run
    this.policyEvaluationResult = policyEvaluationResult
  }

  Run getRun() {
    return run;
  }

  def getApplicationId() {
    return this.applicationId
  }

  def getIqStage() {
    return this.iqStage
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
          component.componentName = getComponentName(fact)
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
        if (failedCurrent > 0) {
          report.failedActionComponents++
        }
        if (warnCurrent > 0) {
          report.warnActionComponents++
        }
      }
    }

    report.components = componentsMap.values().sort { -it.policyLevel }
    return report
  }

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

  SuccessResult getSuccessReesult() {
    if (Math.random() > 0.5){
      return new SuccessResult(SPACE_SHIP_ALT, IQ_SPACE_SHIP_PNG, IQ_SPACE_SHIP_SUCCESS_MESSAGE)
    }
    else {
      return new SuccessResult(BOAT_ALT, IQ_BOAT_PNG, IQ_BOAT_SUCCESS_MESSAGE)
    }
  }

  class SuccessResult
  {
    String alt

    String image

    String message

    SuccessResult() {
    }

    SuccessResult(final String alt, final String image, final String message) {
      this.alt = alt
      this.image = image
      this.message = message
    }
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
