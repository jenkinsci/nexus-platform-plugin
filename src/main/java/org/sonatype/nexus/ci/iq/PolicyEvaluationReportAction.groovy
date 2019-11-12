package org.sonatype.nexus.ci.iq


import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation
import com.sonatype.nexus.api.iq.ComponentFact
import com.sonatype.nexus.api.iq.ConditionFact
import com.sonatype.nexus.api.iq.ConstraintFact
import com.sonatype.nexus.api.iq.PolicyAlert

import hudson.model.HealthReport
import hudson.model.HealthReportingAction
import hudson.model.Project
import hudson.model.Run

class PolicyEvaluationReportAction implements HealthReportingAction {

  private static final String CI_MAVEN_FORMAT = "maven"
  private static final String CI_A_NAME_FORMAT = "a-name"

  private final Run run
  private ApplicationPolicyEvaluation policyEvaluationResult
  private Project project

  PolicyEvaluationReportAction(Run run, final ApplicationPolicyEvaluation policyEvaluationResult) {
    this.run = run;
    this.policyEvaluationResult = policyEvaluationResult
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
    } else if (policyLevel > 3) {
      return 'orange'
    } else if (policyLevel > 1){
      return 'yellow'
    } else if (policyLevel == 1) {
      return 'blue'
    }

    return 'lightblue'
  }

  def getActionIcon() {
    return "<svg xmlns='http://www.w3.org/2000/svg' class='iq-failed-icon' height='16' focusable='false' viewBox='0 0 512 512' width='16'><path d='M504 256c0 136.997-111.043 248-248 248S8 392.997 8 256C8 119.083 119.043 8               256 8s248 111.083 248 248zm-248 50c-25.405 0-46 20.595-46 46s20.595 46 46 46 46-20.595               46-46-20.595-46-46-46zm-43.673-165.346l7.418 136c.347 6.364 5.609 11.346 11.982               11.346h48.546c6.373 0 11.635-4.982               11.982-11.346l7.418-136c.375-6.874-5.098-12.654-11.982-12.654h-63.383c-6.884 0-12.356 5.78-11.981 12.654z' fill='#bc012f'></path></svg>"
  }

  List<PolicyAlert> getAlerts() {
    return this.policyEvaluationResult.policyAlerts
  }

  List<ReportComponent> getReport() {
    Map<String, ReportComponent> report = new HashMap<>()

    for (PolicyAlert alert: this.policyEvaluationResult.policyAlerts) {
      if (alert.actions?.size() == 0) {
        continue
      }

      ReportComponent component = new ReportComponent()
      component.policyName = alert.trigger.policyName
      component.policyLevel = alert.trigger.threatLevel
      component.constraints = new ArrayList<>()

      for (ComponentFact fact : alert.trigger.componentFacts) {
        if (fact.componentIdentifier) {
          component.componentName = getComponentName(fact);
        }
        for (ConstraintFact constraintFact: fact.constraintFacts) {
          Constraint constraint = new Constraint(constraintFact.constraintName,
              component.policyName, component.policyLevel, alert.actions[0]?.actionTypeId)
          for (ConditionFact conditionFact: constraintFact.conditionFacts) {
            constraint.conditions.add(new Condition(conditionFact.summary, conditionFact.reason))
          }
          component.constraints.add(constraint)
        }
      }
      ReportComponent comp = report.get(component.getComponentName())
      if (comp) {
        comp.getConstraints().addAll(component.getConstraints())
        if (comp.policyLevel < component.policyLevel) {
          comp.policyLevel = component.policyLevel
        }
      } else {
        report.put(component.getComponentName(), component)
      }
    }

    return report.values().sort{-it.policyLevel}
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

  private String getComponentName(ComponentFact fact) {
    if (fact.componentIdentifier.format == CI_MAVEN_FORMAT) {
      return "$fact.componentIdentifier.coordinates.groupId : $fact.componentIdentifier.coordinates.artifactId : " +
          "$fact.componentIdentifier.coordinates.version"
    } else if (fact.componentIdentifier.format == CI_A_NAME_FORMAT) {
      return fact.componentIdentifier.coordinates.name
    }

    return "Unknown Component with Unknown Format"
  }

  class ReportComponent {
    String componentName
    String policyName
    Integer policyLevel
    List<Constraint> constraints
  }

  class Condition {
    String summary
    String reason

    Condition() {}
    Condition(final String summary, final String reason) {
      this.summary = summary
      this.reason = reason
    }
  }

  class Constraint {
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
    return '/plugin/nexus-jenkins-plugin/images/24x24/nexus-iq.png'
  }

  @Override
  String getDisplayName() {
    return 'report will be here'
  }

  @Override
  String getUrlName() {
    return 'iqreport'
  }

  @Override
  HealthReport getBuildHealth() {
    return null
  }
}
