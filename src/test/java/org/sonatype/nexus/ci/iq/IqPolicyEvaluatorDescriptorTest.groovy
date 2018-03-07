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

import org.sonatype.nexus.ci.config.NxiqConfiguration
import org.sonatype.nexus.ci.util.FormUtil
import org.sonatype.nexus.ci.util.IqUtil

import hudson.model.Job
import hudson.util.FormValidation.Kind
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

abstract class IqPolicyEvaluatorDescriptorTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  abstract IqPolicyEvaluatorDescriptor getDescriptor()

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
      def descriptor = getDescriptor()

    when:
      "validating application ID $applicationId"
      def validation = descriptor.doCheckListAppId(applicationId)

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

  def 'it validates that application items are filled'() {
    setup:
      def descriptor = getDescriptor()
      GroovyMock(IqUtil, global: true)
      def job = Mock(Job)

    when:
      descriptor.doFillListAppIdItems('', job)

    then:
      1 * IqUtil.doFillIqApplicationItems('', job)
  }

  def 'it uses custom credentials for application items'() {
    setup:
      def descriptor = getDescriptor()
      GroovyMock(IqUtil, global: true)
      def job = Mock(Job)

    when:
      descriptor.doFillListAppIdItems('credentialsId', job)

    then:
      1 * IqUtil.doFillIqApplicationItems('credentialsId', job)
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
      GroovyMock(FormUtil, global: true)
      GroovyMock(NxiqConfiguration, global: true)
      NxiqConfiguration.serverUrl >> URI.create("http://server/path")
      NxiqConfiguration.credentialsId >> 'credentialsId'
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
      def buildStep = new IqPolicyEvaluatorBuildStep(null, null, null, null, null, null, null, 'jobSpecificCredentialsId')

    then:
      buildStep.jobCredentialsId == 'jobSpecificCredentialsId'
  }
}
