/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.iq

import com.sonatype.nexus.api.iq.IqClient
import com.sonatype.nexus.api.iq.PolicyEvaluationResult
import com.sonatype.nexus.ci.config.NxiqConfiguration
import com.sonatype.nexus.ci.iq.IqPolicyEvaluatorBuildStep.PolicyEvaluatorDescriptorImpl
import com.sonatype.nexus.ci.util.FormUtil
import com.sonatype.nexus.ci.util.IqUtil

import com.google.common.base.Optional
import hudson.FilePath
import hudson.Launcher
import hudson.model.Run
import hudson.model.TaskListener
import hudson.util.FormValidation.Kind
import spock.lang.Specification

import static java.util.Collections.emptyList

class IqPolicyEvaluatorBuildStepTest
    extends Specification
{
  def 'it performs scan in build step'() {
    setup:
      GroovyMock(IqClientFactory, global: true)
      GroovyMock(IqApplicationEvaluatorFactory, global: true)
      def mockClient = Mock(IqClient)
      def iqPolicyEvaluator = Mock(IqApplicationEvaluator)
      def workspace = new FilePath(new File("/tmp/path"))
      IqClientFactory.getIqClient() >> mockClient
      IqApplicationEvaluatorFactory.getPolicyEvaluator(mockClient) >> iqPolicyEvaluator
      def buildStep = new IqPolicyEvaluatorBuildStep("stage", "appId", [new ScanPattern("*.jar")], false, "131-cred")
      def result = new PolicyEvaluationResult(0, 0, 0, 0, emptyList(), false)

    when:
      buildStep.perform(Mock(Run), workspace, Mock(Launcher), Mock(TaskListener))

    then:
      1 * iqPolicyEvaluator.performScan("appId", "stage", ["*.jar"], workspace) >> result
  }

  def 'it falls back to default scan targets when none are provided'() {
    setup:
      GroovyMock(IqClientFactory, global: true)
      GroovyMock(IqApplicationEvaluatorFactory, global: true)
      def mockClient = Mock(IqClient)
      def iqPolicyEvaluator = Mock(IqApplicationEvaluator)
      def workspace = new FilePath(new File("/tmp/path"))
      IqClientFactory.getIqClient() >> mockClient
      IqApplicationEvaluatorFactory.getPolicyEvaluator(mockClient) >> iqPolicyEvaluator
      def buildStep = new IqPolicyEvaluatorBuildStep("stage", "appId", [], false, "131-cred")
      def result = new PolicyEvaluationResult(0, 0, 0, 0, emptyList(), false)
      def expectedPatterns = ["**/*.jar", "**/*.war", "**/*.ear", "**/*.zip", "**/*.tar.gz"]

    when:
      buildStep.perform(Mock(Run), workspace, Mock(Launcher), Mock(TaskListener))

    then:
      1 * iqPolicyEvaluator.performScan("appId", "stage", expectedPatterns, workspace) >> result
  }

  def 'it accepts all job types'() {
    setup:
      def descriptor = new PolicyEvaluatorDescriptorImpl()

    expect:
      "job type is $jobType"
      expected == descriptor.isApplicable(jobType)

    where:
      jobType                             | expected
      hudson.model.FreeStyleProject.class | true
      Object                              | true
      null                                | true
  }

  def 'it validates that stage is required'() {
    setup:
      def descriptor = new PolicyEvaluatorDescriptorImpl()

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
      def descriptor = new PolicyEvaluatorDescriptorImpl()

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
      def descriptor = new PolicyEvaluatorDescriptorImpl()

    when:
      "validating application ID $applicationId"
      def validation = descriptor.doCheckIqApplication(applicationId)

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
      def descriptor = new PolicyEvaluatorDescriptorImpl()

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

  def 'it validates that application items are filled'() {
    setup:
      def descriptor = new PolicyEvaluatorDescriptorImpl()
      GroovyMock(IqUtil, global: true)

    when:
      descriptor.doFillIqApplicationItems()

    then:
      1 * IqUtil.doFillIqApplicationItems()
  }

  def 'it validates that stage items are filled'() {
    setup:
      def descriptor = new PolicyEvaluatorDescriptorImpl()
      GroovyMock(IqUtil, global: true)

    when:
      descriptor.doFillIqStageItems()

    then:
      1 * IqUtil.doFillIqStageItems()
  }

  def 'it validates that credentials items are filled'() {
    setup:
      def descriptor = new PolicyEvaluatorDescriptorImpl()
      GroovyMock(NxiqConfiguration, global: true)
      GroovyMock(FormUtil, global: true)
      NxiqConfiguration.serverUrl >> "https://server/url"
      NxiqConfiguration.credentialsId >> "123-cred-456"

    when:
      descriptor.doFillJobCredentialsIdItems()

    then:
      1 * FormUtil.buildCredentialsItems("https://server/url")
  }
}
