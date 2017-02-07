/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import com.sonatype.insight.scan.model.Scan
import com.sonatype.nexus.api.iq.Action
import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation
import com.sonatype.nexus.api.iq.internal.InternalIqClient
import com.sonatype.nexus.api.iq.internal.InternalIqClientBuilder
import com.sonatype.nexus.api.iq.scan.ScanResult
import com.sonatype.nexus.ci.config.GlobalNexusConfiguration
import com.sonatype.nexus.ci.config.NxiqConfiguration
import com.sonatype.nexus.ci.config.NxrmConfiguration

import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import hudson.model.FreeStyleProject
import hudson.model.Result
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

import static com.sonatype.nexus.ci.iq.TestDataGenerators.createAlert

class IqPolicyEvaluatorIntegrationTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  List<? extends NxrmConfiguration> nxrmConfiguration

  List<NxiqConfiguration> nxiqConfiguration

  UsernamePasswordCredentials credentials

  InternalIqClientBuilder iqClientBuilder

  InternalIqClient iqClient

  def setup() {
    GroovyMock(InternalIqClientBuilder, global: true)
    iqClientBuilder = Mock()
    InternalIqClientBuilder.create() >> iqClientBuilder
    iqClientBuilder.withProxyConfig(_) >> iqClientBuilder
    iqClientBuilder.withServerConfig(_) >> iqClientBuilder
    iqClientBuilder.withLogger(_) >> iqClientBuilder
    iqClientBuilder.withInstanceId(_) >> iqClientBuilder
    iqClient = Mock()
    iqClientBuilder.build() >> iqClient
  }

  def 'Pipeline build should return result when build is successful'() {
    given: 'a jenkins project'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      configureJenkins()

    when: 'the nexus policy evaluator is executed'
      project.definition = new CpsFlowDefinition('node {\n' +
          'writeFile file: \'dummy.txt\', text: \'dummy\'\n' +
          'def result = nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: \'app\', ' +
          'iqStage: \'stage\'\n' +
          'echo "url:" + result.applicationCompositionReportUrl\n' +
          'echo "affected:" + result.affectedComponentCount\n' +
          'echo "critical:" + result.criticalComponentCount\n' +
          'echo "severe:" + result.severeComponentCount\n' +
          'echo "moderate:" + result.moderateComponentCount\n' +
          'echo "url:" + result.applicationCompositionReportUrl\n' +
          'echo "reevaluation:" + result.reevaluation\n' +
          'echo "alerts:" + result.policyAlerts' +
          '}\n')
      def build = project.scheduleBuild2(0).get()

    then: 'the application is scanned and evaluated'
      1 * iqClient.scan(*_) >> new ScanResult(new Scan(), File.createTempFile('dummy-scan', '.xml.gz'))
      1 * iqClient.evaluateApplication(*_) >> new ApplicationPolicyEvaluation(0, 1, 2, 3, [], false,
          'http://server/link/to/report')

    then: 'the expected result is returned'
      jenkins.assertBuildStatusSuccess(build)
      with(build.getLog(100)) {
        it.contains('url:http://server/link/to/report')
        it.contains('affected:0')
        it.contains('critical:1')
        it.contains('severe:2')
        it.contains('moderate:3')
        it.contains('reevaluation:false')
        it.contains('alerts:[]')
      }
  }

  def 'Freestyle build (happy path)'() {
    given: 'a jenkins project'
      FreeStyleProject project = jenkins.createFreeStyleProject()
      project.buildersList.add(new IqPolicyEvaluatorBuildStep('stage', 'app', [], false, 'cred-id'))
      configureJenkins()

    when: 'the build is scheduled'
      def build = project.scheduleBuild2(0).get()

    then: 'the application is scanned and evaluated'
      1 * iqClient.scan(*_) >> new ScanResult(new Scan(), File.createTempFile('dummy-scan', '.xml.gz'))
      1 * iqClient.evaluateApplication(*_) >> new ApplicationPolicyEvaluation(0, 1, 2, 3, [], false,
          'http://server/link/to/report')

    then: 'the return code is successful'
      jenkins.assertBuildStatusSuccess(build)
  }

  def 'Pipeline build should return null and set status to unstable when build fails with network error'() {
    given: 'a jenkins project'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      configureJenkins()

    when: 'the nexus policy evaluator is executed'
      project.definition = new CpsFlowDefinition('node {\n' +
          'writeFile file: \'dummy.txt\', text: \'dummy\'\n' +
          'def result = nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: \'app\', ' +
          'iqStage: \'stage\'\n' +
          'echo \'result-after-failure:\' + result' +
          '}\n')
      def build = project.scheduleBuild2(0).get()

    then: 'an exception is thrown when getting proprietary config from IQ server'
      1 * iqClient.getProprietaryConfigForApplicationEvaluation('app') >> { throw new IOException("BANG!") }

    then: 'the build status is unstable and the result is null'
      jenkins.assertBuildStatus(Result.UNSTABLE, build)
      build.getLog(100).contains('result-after-failure:null')
  }

  def 'Freestyle build should set build status to unstable when network error occurs'() {
    given: 'a jenkins project'
      def failBuildOnNetworkError = false
      FreeStyleProject project = jenkins.createFreeStyleProject()
      project.buildersList.add(new IqPolicyEvaluatorBuildStep('stage', 'app', [], failBuildOnNetworkError, 'cred-id'))
      configureJenkins()

    when: 'the build is scheduled'
      def build = project.scheduleBuild2(0).get()

    then: 'an exception is thrown when getting proprietary config from IQ server'
      1 * iqClient.getProprietaryConfigForApplicationEvaluation('app') >> { throw new IOException("BANG!") }

    then: 'the return code is successful'
      jenkins.assertBuildStatus(Result.UNSTABLE, build)
  }

  def 'Pipeline build fails when an exception other than IOException occurs'() {
    given: 'a jenkins project'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      configureJenkins()

    when: 'the nexus policy evaluator is executed'
      project.definition = new CpsFlowDefinition('node {\n' +
          'writeFile file: \'dummy.txt\', text: \'dummy\'\n' +
          'nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: \'app\', ' +
          'iqStage: \'stage\'\n' +
          '}\n')
      def build = project.scheduleBuild2(0).get()

    then: 'an exception is thrown when getting proprietary config from IQ server'
      1 * iqClient.getProprietaryConfigForApplicationEvaluation('app') >> { throw new NullPointerException("CRASH!") }

    then: 'the build fails'
     jenkins.assertBuildStatus(Result.FAILURE, build)
  }

  def 'Freestyle build should set build status to failed when an exception other than IOException occurs'() {
    given: 'a jenkins project'
      def failBuildOnNetworkError = false
      FreeStyleProject project = jenkins.createFreeStyleProject()
      project.buildersList.add(new IqPolicyEvaluatorBuildStep('stage', 'app', [], failBuildOnNetworkError, 'cred-id'))
      configureJenkins()

    when: 'the build is scheduled'
      def build = project.scheduleBuild2(0).get()

    then: 'an exception is thrown when getting proprietary config from IQ server'
      1 * iqClient.getProprietaryConfigForApplicationEvaluation('app') >> { throw new NullPointerException("BANG!") }

    then: 'the return code is successful'
      jenkins.assertBuildStatus(Result.FAILURE, build)
  }

  def 'Pipeline build should fail when policy violations are present'() {
    setup: 'global server URL and globally configured credentials'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      configureJenkins()

    when: 'the nexus policy evaluator is executed'
      project.definition = new CpsFlowDefinition('node {\n' +
          'writeFile file: \'dummy.txt\', text: \'dummy\'\n' +
          'def result = nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: \'app\',' +
          'iqStage: \'stage\'\n' +
          'echo "url:" + result.applicationCompositionReportUrl\n' +
          'echo "affected:" + result.affectedComponentCount\n' +
          'echo "critical:" + result.criticalComponentCount\n' +
          'echo "severe:" + result.severeComponentCount\n' +
          'echo "moderate:" + result.moderateComponentCount\n' +
          'echo "url:" + result.applicationCompositionReportUrl\n' +
          'echo "reevaluation:" + result.reevaluation\n' +
          'echo "alerts:" + result.policyAlerts' +
          '}\n')
      def build = project.scheduleBuild2(0).get()

    then: 'the application is scanned and evaluated'
      1 * iqClient.scan(*_) >> new ScanResult(new Scan(), File.createTempFile('dummy-scan', '.xml.gz'))
      1 * iqClient.evaluateApplication(*_) >> new ApplicationPolicyEvaluation(0, 1, 2, 3,
          [createAlert(Action.ID_FAIL)], false, 'http://server/link/to/report')

    then: 'the build fails'
      jenkins.assertBuildStatus(Result.FAILURE, build)
      with(build.getLog(100)) {
        it.contains('url:http://server/link/to/report')
        it.contains('affected:0')
        it.contains('critical:1')
        it.contains('severe:2')
        it.contains('moderate:3')
        it.contains('reevaluation:false')
        it =~ /alerts:\[.+]/
      }
  }

  def 'Freestyle build should fail when policy violations are present'() {
    given: 'a jenkins project'
      def failBuildOnNetworkError = false
      FreeStyleProject project = jenkins.createFreeStyleProject()
      project.buildersList.add(new IqPolicyEvaluatorBuildStep('stage', 'app', [], failBuildOnNetworkError, 'cred-id'))
      configureJenkins()

    when: 'the build is scheduled'
      def build = project.scheduleBuild2(0).get()

    then: 'the application is scanned and evaluated'
      1 * iqClient.scan(*_) >> new ScanResult(new Scan(), File.createTempFile('dummy-scan', '.xml.gz'))
      1 * iqClient.evaluateApplication(*_) >> new ApplicationPolicyEvaluation(0, 1, 2, 3,
          [createAlert(Action.ID_FAIL)], false, 'http://server/link/to/report')

    then: 'the build fails'
      jenkins.assertBuildStatus(Result.FAILURE, build)
  }

  def configureJenkins() {
    nxiqConfiguration = [new NxiqConfiguration('http://server/url', false, 'cred-id')]
    GlobalNexusConfiguration.globalNexusConfiguration.iqConfigs = nxiqConfiguration
    GlobalNexusConfiguration.globalNexusConfiguration.nxrmConfigs = nxrmConfiguration
    credentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, 'cred-id', 'name', 'user', 'password')
    CredentialsProvider.lookupStores(jenkins.jenkins).first().addCredentials(Domain.global(), credentials)
  }
}
