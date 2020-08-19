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

import java.security.SecureRandom

import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation

import hudson.model.Run
import jenkins.model.RunAction2

class PolicyEvaluationReportAction
    implements RunAction2, Serializable
{
  private static final String ICON_NAME = '/plugin/nexus-jenkins-plugin/images/24x24/nexus-iq.png'

  private static final String IQ_REPORT_NAME = 'iqreport'

  private static final String MENU_REPORT_TITLE = 'Nexus IQ Build Report'

  private static final String IQ_SPACE_SHIP_PNG = '/plugin/nexus-jenkins-plugin/images/sonatype-iq-rocketship.png'
  private static final String IQ_SPACE_SHIP_SUCCESS_MESSAGE = 'We\'re all clear!'
  private static final String SPACE_SHIP_ALT = 'A Space Ship'

  private static final String IQ_BOAT_PNG = '/plugin/nexus-jenkins-plugin/images/sonatype-iq-boat.png'
  private static final String IQ_BOAT_SUCCESS_MESSAGE = 'We\'re smooth sailing!'
  private static final String BOAT_ALT = 'A Boat'

  private transient Run run

  private final ApplicationPolicyEvaluation policyEvaluationResult

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
    return run
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

  def getReport() {
    return PolicyEvaluationReportUtil.parseApplicationPolicyEvaluation(this.policyEvaluationResult)
  }

  SuccessResult getSuccessResult() {
    if (new SecureRandom().nextInt(100) > 50) {
      return new SuccessResult(alt: SPACE_SHIP_ALT, image: IQ_SPACE_SHIP_PNG, message: IQ_SPACE_SHIP_SUCCESS_MESSAGE)
    }
    else {
      return new SuccessResult(alt: BOAT_ALT, image:  IQ_BOAT_PNG, message:  IQ_BOAT_SUCCESS_MESSAGE)
    }
  }

  class SuccessResult
  {
    String alt
    String image
    String message
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
    this.run = run
  }
}
