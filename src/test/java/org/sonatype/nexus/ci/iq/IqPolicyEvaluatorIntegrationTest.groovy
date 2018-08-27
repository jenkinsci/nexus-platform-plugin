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

import com.sonatype.insight.scan.model.Scan
import com.sonatype.nexus.api.exception.IqClientException
import com.sonatype.nexus.api.iq.Action
import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation
import com.sonatype.nexus.api.iq.internal.InternalIqClient
import com.sonatype.nexus.api.iq.internal.InternalIqClientBuilder
import com.sonatype.nexus.api.iq.scan.ScanResult

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.NxiqConfiguration
import org.sonatype.nexus.ci.config.NxrmConfiguration

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
import spock.lang.Unroll

import static org.sonatype.nexus.ci.iq.TestDataGenerators.createAlert

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

  def 'Declarative pipeline build successful with mandatory parameters'() {
    given: 'a jenkins project'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      configureJenkins()

    when: 'the nexus policy evaluator is executed'
      project.definition = new CpsFlowDefinition('' +
          'pipeline { \n' +
            'agent any \n' +
              'stages { \n' +
                'stage("Example") { \n' +
                'steps { \n' +
                  'writeFile file: \'dummy.txt\', text: \'dummy\'\n' +
                  'nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: \'app\', ' +
                  'iqStage: \'stage\'\n'+
                '} \n' +
              '} \n' +
            '} \n' +
          '} \n')
      def build = project.scheduleBuild2(0).get()

    then: 'the application is scanned and evaluated'
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.scan(*_) >> new ScanResult(new Scan(), File.createTempFile('dummy-scan', '.xml.gz'))
      1 * iqClient.evaluateApplication(*_) >>
          new ApplicationPolicyEvaluation(0, 1, 2, 3, 0, [], 'http://server/link/to/report')

    and: 'the build is successful'
      jenkins.assertBuildStatusSuccess(build)
  }

  def 'Declarative pipeline build successful with selectedApplication call'() {
    given: 'a jenkins project'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      configureJenkins()

    when: 'the nexus policy evaluator is executed'
      project.definition = new CpsFlowDefinition('''
          pipeline {  
            agent any
              stages {
                stage("Example") {
                  steps { 
                    writeFile file: 'dummy.txt', text: 'dummy'
                    nexusPolicyEvaluation failBuildOnNetworkError: false, 
                      iqApplication: selectedApplication('app'), iqStage: 'stage'
                  }
                }
              }
          }''')
      def build = project.scheduleBuild2(0).get()

    then: 'the application with application id "app" is scanned and evaluated'
      1 * iqClient.verifyOrCreateApplication('app') >> true
      1 * iqClient.scan(*_) >> new ScanResult(new Scan(), File.createTempFile('dummy-scan', '.xml.gz'))
      1 * iqClient.evaluateApplication(*_) >>
          new ApplicationPolicyEvaluation(0, 1, 2, 3, 0, [], 'http://server/link/to/report')

    and: 'the build is successful'
      jenkins.assertBuildStatusSuccess(build)
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
          'echo "alerts:" + result.policyAlerts' +
          '}\n')
      def build = project.scheduleBuild2(0).get()

    then: 'the application is scanned and evaluated'
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.scan(*_) >> new ScanResult(new Scan(), File.createTempFile('dummy-scan', '.xml.gz'))
      1 * iqClient.evaluateApplication(*_) >> new ApplicationPolicyEvaluation(0, 1, 2, 3, 0, [],
          'http://server/link/to/report')

    then: 'the expected result is returned'
      jenkins.assertBuildStatusSuccess(build)
      with(build.getLog(100)) {
        it.contains('url:http://server/link/to/report')
        it.contains('affected:0')
        it.contains('critical:1')
        it.contains('severe:2')
        it.contains('moderate:3')
        it.contains('alerts:[]')
      }
  }

  def 'Freestyle build (happy path)'() {
    given: 'a jenkins project'
      FreeStyleProject project = jenkins.createFreeStyleProject()
      project.buildersList.add(new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('app'), [], [], false, 'cred-id'))
      configureJenkins()

    when: 'the build is scheduled'
      def build = project.scheduleBuild2(0).get()

    then: 'the application is scanned and evaluated'
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.scan(*_) >> new ScanResult(new Scan(), File.createTempFile('dummy-scan', '.xml.gz'))
      1 * iqClient.evaluateApplication(*_) >> new ApplicationPolicyEvaluation(0, 1, 2, 3, 0, [],
          'http://server/link/to/report')

    then: 'the return code is successful'
      jenkins.assertBuildStatusSuccess(build)
  }

  @Unroll
  def 'Pipeline build should return null and set status to unstable when build fails with network error with failBuildOnNetworkError #description'() {
    given: 'a jenkins project'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      configureJenkins()

    when: 'the nexus policy evaluator is executed'
      project.definition = new CpsFlowDefinition('node {\n' +
          'writeFile file: \'dummy.txt\', text: \'dummy\'\n' +
          "def result = nexusPolicyEvaluation ${failBuildOnNetworkErrorScript} iqApplication: \'app\', " +
          'iqStage: \'stage\'\n' +
          'echo \'result-after-failure:\' + result' +
          '}\n')
      def build = project.scheduleBuild2(0).get()

    then: 'an exception is thrown when getting proprietary config from IQ server'
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.getProprietaryConfigForApplicationEvaluation('app') >> {
        throw new IqClientException("ERROR", new IOException("BANG!"))
      }

    then: 'the build status is unstable and the result is null'
      jenkins.assertBuildStatus(Result.UNSTABLE, build)
      build.getLog(100).contains('result-after-failure:null')
      build.getLog(100).contains('com.sonatype.nexus.api.exception.IqClientException: ERROR')

    where:
      description     | failBuildOnNetworkErrorScript
      'false'         | 'failBuildOnNetworkError: false,'
      'not supplied'  | ''
  }

  def 'Pipeline build should set status to fail when build fails with network error with failBuildOnNetworkError true'() {
    given: 'a jenkins project'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      configureJenkins()

    when: 'the nexus policy evaluator is executed'
      project.definition = new CpsFlowDefinition('node {\n' +
          'nexusPolicyEvaluation failBuildOnNetworkError: true, iqApplication: \'app\', iqStage: \'stage\'\n' +
          '}\n')
      def build = project.scheduleBuild2(0).get()

    then: 'an exception is thrown when getting proprietary config from IQ server'
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.getProprietaryConfigForApplicationEvaluation('app') >> {
        throw new IqClientException("ERROR", new IOException("BANG!"))
      }

    then: 'the build status is failure and the error is logged'
      jenkins.assertBuildStatus(Result.FAILURE, build)
      build.getLog(100).contains('com.sonatype.nexus.api.exception.IqClientException: ERROR')
  }

  def 'Freestyle build should set build status to unstable when network error occurs with failBuildOnNetworkError false'() {
    given: 'a jenkins project'
      def failBuildOnNetworkError = false
      FreeStyleProject project = jenkins.createFreeStyleProject()
      project.buildersList.add(new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('app'), [], [], failBuildOnNetworkError, 'cred-id'))
      configureJenkins()

    when: 'the build is scheduled'
      def build = project.scheduleBuild2(0).get()

    then: 'an exception is thrown when getting proprietary config from IQ server'
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.getProprietaryConfigForApplicationEvaluation('app') >> {
        throw new IqClientException("ERROR", new IOException("BANG!"))
      }

    then: 'the return code is unstable and the error is logged'
      jenkins.assertBuildStatus(Result.UNSTABLE, build)
      build.getLog(100).contains('com.sonatype.nexus.api.exception.IqClientException: ERROR')
  }

  def 'Freestyle build should set status to fail when build fails with network error with failBuildOnNetworkError true'() {
    given: 'a jenkins project'
      FreeStyleProject project = jenkins.createFreeStyleProject()
      project.buildersList.add(new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('app'), [], [], true, 'cred-id'))
      configureJenkins()

    when: 'the build is scheduled'
      def build = project.scheduleBuild2(0).get()

    then: 'an exception is thrown when getting proprietary config from IQ server'
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.getProprietaryConfigForApplicationEvaluation('app') >> {
        throw new IqClientException("ERROR", new IOException("BANG!"))
      }

    then: 'the build status is failure and the error is logged'
      jenkins.assertBuildStatus(Result.FAILURE, build)
      build.getLog(100).contains('com.sonatype.nexus.api.exception.IqClientException: ERROR')
  }

  @Unroll
  def 'Pipeline build fails when an exception other than IqClientException with IOException occurs with #description'() {
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
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.getProprietaryConfigForApplicationEvaluation('app') >> { throw exception }

    then: 'the build fails'
     jenkins.assertBuildStatus(Result.FAILURE, build)

    where:
      description                   | exception
      'NPE'                         | new NullPointerException("CRASH!")
      'IqClientException with NPE'  | new IqClientException("ERROR", new NullPointerException("CRASH!"))
  }

  def 'Freestyle build should set build status to failed when an exception other than IqClientException with IOException occurs with #description'() {
    given: 'a jenkins project'
      def failBuildOnNetworkError = false
      FreeStyleProject project = jenkins.createFreeStyleProject()
      project.buildersList.add(new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('app'), [], [], failBuildOnNetworkError, 'cred-id'))
      configureJenkins()

    when: 'the build is scheduled'
      def build = project.scheduleBuild2(0).get()

    then: 'an exception is thrown when getting proprietary config from IQ server'
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.getProprietaryConfigForApplicationEvaluation('app') >> { throw exception }

    then: 'the return code is successful'
      jenkins.assertBuildStatus(Result.FAILURE, build)

    where:
      description                   | exception
      'NPE'                         | new NullPointerException("CRASH!")
      'IqClientException with NPE'  | new IqClientException("ERROR", new NullPointerException("CRASH!"))
  }

  def 'Pipeline build should fail and stop execution when policy violations are present'() {
    setup: 'global server URL and globally configured credentials'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      configureJenkins()

    when: 'the nexus policy evaluator is executed'
      project.definition = new CpsFlowDefinition('node {\n' +
          'writeFile file: \'dummy.txt\', text: \'dummy\'\n' +
          'def result = nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: \'app\', ' +
          'iqStage: \'stage\'\n' +
          'echo "next" \n' +
          '}\n')
      def build = project.scheduleBuild2(0).get()

    then: 'the application is scanned and evaluated'
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.scan(*_) >> new ScanResult(new Scan(), File.createTempFile('dummy-scan', '.xml.gz'))
      1 * iqClient.evaluateApplication(*_) >> new ApplicationPolicyEvaluation(0, 1, 2, 3, 0,
          [createAlert(Action.ID_FAIL)], 'http://server/link/to/report')

    and: 'the build fails'
      jenkins.assertBuildStatus(Result.FAILURE, build)
      with(build.getLog(100)) {
        !it.contains('next')
      }
  }

  def 'Pipeline build should failure should container policy evaluation results'() {
    setup: 'global server URL and globally configured credentials'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      configureJenkins()

    when: 'the nexus policy evaluator is executed'
      project.definition = new CpsFlowDefinition('' +
          'node { \n' +
            'writeFile file: \'dummy.txt\', text: \'dummy\' \n' +
            'try { \n' +
              'nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: \'sample-app\', ' +
              'iqStage: \'stage\' \n' +
            '} catch (error) { \n' +
              'def result = error.policyEvaluation \n' +
              'echo "url:" + result.applicationCompositionReportUrl\n' +
              'echo "affected:" + result.affectedComponentCount\n' +
              'echo "critical:" + result.criticalComponentCount\n' +
              'echo "severe:" + result.severeComponentCount\n' +
              'echo "moderate:" + result.moderateComponentCount\n' +
              'echo "url:" + result.applicationCompositionReportUrl\n' +
              'echo "alerts:" + result.policyAlerts' +
            '} \n' +
          '}\n')
      def build = project.scheduleBuild2(0).get()

    then: 'the application is scanned and evaluated'
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.scan(*_) >> new ScanResult(new Scan(), File.createTempFile('dummy-scan', '.xml.gz'))
      1 * iqClient.evaluateApplication(*_) >> new ApplicationPolicyEvaluation(0, 1, 2, 3, 0,
          [createAlert(Action.ID_FAIL)], 'http://server/link/to/report')

    and: 'the build fails'
      jenkins.assertBuildStatus(Result.FAILURE, build)
      with(build.getLog(100)) {
        it.contains('url:http://server/link/to/report')
        it.contains('affected:0')
        it.contains('critical:1')
        it.contains('severe:2')
        it.contains('moderate:3')
        it =~ /alerts:\[.+]/
      }
  }

  def 'Freestyle build should fail when policy violations are present'() {
    given: 'a jenkins project'
      def failBuildOnNetworkError = false
      FreeStyleProject project = jenkins.createFreeStyleProject()
      project.buildersList.add(new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('app'), [], [], failBuildOnNetworkError, 'cred-id'))
      configureJenkins()

    when: 'the build is scheduled'
      def build = project.scheduleBuild2(0).get()

    then: 'the application is scanned and evaluated'
      1 * iqClient.verifyOrCreateApplication(*_) >> true
      1 * iqClient.scan(*_) >> new ScanResult(new Scan(), File.createTempFile('dummy-scan', '.xml.gz'))
      1 * iqClient.evaluateApplication(*_) >> new ApplicationPolicyEvaluation(0, 1, 2, 3, 0,
          [createAlert(Action.ID_FAIL)], 'http://server/link/to/report')

    then: 'the build fails'
      jenkins.assertBuildStatus(Result.FAILURE, build)
  }

  def 'Pipeline build step should fail with exception when mandatory parameter is missing'() {
    given: 'a jenkins project'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      configureJenkins()

    when: 'the nexus policy evaluator is executed without stage'
      project.definition = new CpsFlowDefinition('node {\n' +
          'writeFile file: \'dummy.txt\', text: \'dummy\'\n' +
          'def result = nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: \'app\'\n' +
          'echo \'result-after-failure:\' + result' +
          '}\n')
      def build = project.scheduleBuild2(0).get()

    then: 'the build status is unstable and the result is null'
      jenkins.assertBuildStatus(Result.FAILURE, build)
      build.getLog(99).contains('java.lang.IllegalArgumentException: Arguments iqApplication and iqStage are mandatory')

    when: 'the nexus policy evaluator is executed without application'
      project.definition = new CpsFlowDefinition('node {\n' +
          'writeFile file: \'dummy.txt\', text: \'dummy\'\n' +
          'def result = nexusPolicyEvaluation failBuildOnNetworkError: false, iqStage: \'build\'\n' +
          'echo \'result-after-failure:\' + result' +
          '}\n')
      build = project.scheduleBuild2(0).get()

    then: 'the build status is unstable and the result is null'
      jenkins.assertBuildStatus(Result.FAILURE, build)
      build.getLog(99).contains('java.lang.IllegalArgumentException: Arguments iqApplication and iqStage are mandatory')
  }

  def 'Freestyle build should fail when verify is false'() {
    given: 'a jenkins project'
      def failBuildOnNetworkError = false
      FreeStyleProject project = jenkins.createFreeStyleProject()
      project.buildersList.add(new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('app'), [], [], failBuildOnNetworkError, 'cred-id'))
      configureJenkins()

    when: 'the build is scheduled'
      def build = project.scheduleBuild2(0).get()

    then: 'the application is scanned and evaluated'
      1 * iqClient.verifyOrCreateApplication(*_) >> false

    then: 'the build fails'
      jenkins.assertBuildStatus(Result.FAILURE, build)
  }

  def 'Freestyle build should fail when validate server version fails'() {
    given: 'a jenkins project'
      def failBuildOnNetworkError = false
      FreeStyleProject project = jenkins.createFreeStyleProject()
      project.buildersList.add(new IqPolicyEvaluatorBuildStep('stage', new SelectedApplication('app'), [], [], failBuildOnNetworkError, 'cred-id'))
      configureJenkins()

    when: 'the build is scheduled'
      def build = project.scheduleBuild2(0).get()

    then: 'the application is evaluated and the server version check fails'
      1 * iqClient.validateServerVersion(*_) >> { throw new Exception("server version check failed") }

    then: 'the build fails'
      jenkins.assertBuildStatus(Result.FAILURE, build)
  }


  def 'Pipeline build should fail and stop execution when verify is false'() {
    setup: 'global server URL and globally configured credentials'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      configureJenkins()

    when: 'the nexus policy evaluator is executed'
      project.definition = new CpsFlowDefinition('node {\n' +
          'writeFile file: \'dummy.txt\', text: \'dummy\'\n' +
          'def result = nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: \'app\', ' +
          'iqStage: \'stage\'\n' +
          'echo "next" \n' +
          '}\n')
      def build = project.scheduleBuild2(0).get()

    then: 'the application is scanned and evaluated'
      1 * iqClient.verifyOrCreateApplication(*_) >> false

    and: 'the build fails'
      jenkins.assertBuildStatus(Result.FAILURE, build)
      with(build.getLog(100)) {
        !it.contains('next')
      }
  }

  def 'Pipeline build should fail and stop execution when validate server version fails'() {
    setup: 'global server URL and globally configured credentials'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      configureJenkins()

    when: 'the nexus policy evaluator is executed'
      project.definition = new CpsFlowDefinition('node {\n' +
          'writeFile file: \'dummy.txt\', text: \'dummy\'\n' +
          'def result = nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: \'app\', ' +
          'iqStage: \'stage\'\n' +
          'echo "next" \n' +
          '}\n')
      def build = project.scheduleBuild2(0).get()

    then: 'the application is evaluated and server version is checked'
      1 * iqClient.validateServerVersion(*_) >> { throw new Exception("server version check failed") }

    and: 'the build fails'
      jenkins.assertBuildStatus(Result.FAILURE, build)
      with(build.getLog(100)) {
        !it.contains('next')
      }
  }

  def configureJenkins() {
    nxiqConfiguration = [new NxiqConfiguration('http://server/url', 'cred-id')]
    GlobalNexusConfiguration.globalNexusConfiguration.iqConfigs = nxiqConfiguration
    GlobalNexusConfiguration.globalNexusConfiguration.nxrmConfigs = nxrmConfiguration
    credentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, 'cred-id', 'name', 'user', 'password')
    CredentialsProvider.lookupStores(jenkins.jenkins).first().addCredentials(Domain.global(), credentials)
  }
}
