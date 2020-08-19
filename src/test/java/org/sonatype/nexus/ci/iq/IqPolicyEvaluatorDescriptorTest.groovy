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

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.NxiqConfiguration
import org.sonatype.nexus.ci.util.FormUtil
import org.sonatype.nexus.ci.util.IqUtil

import hudson.model.Job
import hudson.util.FormValidation.Kind
import hudson.util.ListBoxModel
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification
import spock.lang.Unroll

abstract class IqPolicyEvaluatorDescriptorTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  abstract IqPolicyEvaluatorDescriptor getDescriptor()

  org.sonatype.nexus.ci.iq.SelectedApplication.DescriptorImpl getSelectedApplication()
  {
    return (org.sonatype.nexus.ci.iq.SelectedApplication.DescriptorImpl) jenkins.getInstance().getDescriptor(SelectedApplication.class)
  }

  org.sonatype.nexus.ci.iq.ManualApplication.DescriptorImpl getManualApplication()
  {
    return (org.sonatype.nexus.ci.iq.ManualApplication.DescriptorImpl) jenkins.getInstance().getDescriptor(ManualApplication.class)
  }

  def 'it validates that stage is required'() {
    setup:
      def descriptor = getDescriptor()

    when:
      "validating stage $stage"
      def validation = descriptor.doCheckIqStage(stage)

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml() == message

    where:
      stage   | kind       | message
      ''      | Kind.ERROR | 'Required'
      null    | Kind.ERROR | 'Required'
      'stage' | Kind.OK    | '<div/>'
  }

  def 'it validates that flag failBuildOnNetworkError is required'() {
    setup:
      def descriptor = getDescriptor()

    when:
      "validating failBuildOnNetworkError $failBuildOnNetworkError"
      def validation = descriptor.doCheckFailBuildOnNetworkError(failBuildOnNetworkError)

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml() == message

    where:
      failBuildOnNetworkError | kind       | message
      ''                      | Kind.ERROR | 'Required'
      null                    | Kind.ERROR | 'Required'
      'true'                  | Kind.OK    | '<div/>'
      'false'                 | Kind.OK    | '<div/>'
      'other'                 | Kind.OK    | '<div/>'

  }

  def 'it validates that application ID is required'() {
    setup:
      def descriptor = getSelectedApplication()

    when:
      "validating application ID $applicationId"
      def validation = descriptor.doCheckApplicationId(applicationId)

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml() == message

    where:
      applicationId   | kind       | message
      ''              | Kind.ERROR | 'Required'
      null            | Kind.ERROR | 'Required'
      'applicationId' | Kind.OK    | '<div/>'
  }

  def 'it validates that scan pattern is not required'() {
    setup:
      def descriptor = getDescriptor()

    when:
      "validating instance ID $pattern"
      def validation = descriptor.doCheckScanPattern(pattern)

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml() == message

    where:
      pattern | kind    | message
      ''      | Kind.OK | '<div/>'
      null    | Kind.OK | '<div/>'
      'file'  | Kind.OK | '<div/>'
  }

  def 'it validates that module exclude is not required'() {
    setup:
      def descriptor = getDescriptor()

    when:
      "validating instance ID $pattern"
      def validation = descriptor.doCheckModuleExclude(pattern)

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml() == message

    where:
      pattern | kind    | message
      ''      | Kind.OK | '<div/>'
      null    | Kind.OK | '<div/>'
      'file'  | Kind.OK | '<div/>'
  }

  @Unroll
  def 'it validates that advanced properties are not required'() {
    setup:
      def descriptor = getDescriptor()

    when: "validating advanced properties $pattern"
      def validation = descriptor.doCheckAdvancedProperties(pattern)

    then: "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml() == message

    where:
      pattern | kind    | message
      ''      | Kind.OK | '<div/>'
      null    | Kind.OK | '<div/>'
      'file'  | Kind.OK | '<div/>'
  }



  def 'it validates that application items are filled'() {
    setup:
      def descriptor = getSelectedApplication()
      def job = Mock(Job)
      ListBoxModel appList

    when:
      appList = descriptor.doFillApplicationIdItems('', job)

    then:
      appList.size() == 1
      appList.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      appList.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE
  }

  def 'it uses custom credentials for application items'() {
    setup:
      def descriptor = getSelectedApplication()
      // create a config so that IqUtil.doFillApplicationIdItems will go down the path that uses custom credentials
      GlobalNexusConfiguration.globalNexusConfiguration.iqConfigs = [new NxiqConfiguration('http://server/url',
          null)]
      def job = Mock(Job)

    when:
      descriptor.doFillApplicationIdItems('credentialsId', job)

    then:
      // this is a bit odd, but because we're mixing Groovy and Java classes SelectedApplication, we can't
      // use GroovyMock to verify interactions.  What we are testing here is that IqUtil.doFillIqApplicationItems
      // was invoked with 'credentialsId'.  Since the Jenkins credential hasn't been created
      // IqUtil.doFillIqApplicationItems will throw an exception, and then we can check the exception message
      // to make sure that IqUtil.doFillIqApplicationItems received the expected parameter.
      IllegalArgumentException ex = thrown()
      ex.message == 'No credentials were found for credentials credentialsId'
  }

  def 'it validates that stage items are filled'() {
    setup:
      def descriptor = getDescriptor()
      GroovyMock(IqUtil, global: true)
      def job = Mock(Job)

    when:
      descriptor.doFillIqStageItems('', job)

    then:
      1 * IqUtil.doFillIqStageItems('', job)
  }

  def 'it uses custom credentials for stage items'() {
    setup:
      def descriptor = getDescriptor()
      GroovyMock(IqUtil, global: true)
      def job = Mock(Job)

    when:
      descriptor.doFillIqStageItems('credentialsId', job)

    then:
      1 * IqUtil.doFillIqStageItems('credentialsId', job)
  }

  def 'it validates that credentials items are filled'() {
    setup:
      def descriptor = getDescriptor()
      GlobalNexusConfiguration.globalNexusConfiguration.iqConfigs = [new NxiqConfiguration('http://server/path',
          'credentialsId')]
      GroovyMock(FormUtil, global: true)
      def job = Mock(Job)

    when:
      descriptor.doFillJobCredentialsIdItems(job)

    then:
      1 * FormUtil.newCredentialsItemsListBoxModel("http://server/path", 'credentialsId', job)
  }

  def 'it validates that credentials items are verified'() {
    setup:
      def descriptor = getDescriptor()
      GroovyMock(IqUtil, global: true)
      def job = Mock(Job)

    when:
      descriptor.doVerifyCredentials('credentialsId', job)

    then:
      1 * IqUtil.verifyJobCredentials('credentialsId', job)
  }

  def 'it sets job specific credentials'() {
    setup:
      GroovyMock(NxiqConfiguration, global: true)

    when:
      def buildStep = new IqPolicyEvaluatorBuildStep(null, null, null, null, null, 'jobSpecificCredentialsId', null)

    then:
      buildStep.jobCredentialsId == 'jobSpecificCredentialsId'
  }
}
