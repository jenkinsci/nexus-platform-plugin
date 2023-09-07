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


import com.gargoylesoftware.htmlunit.html.HtmlOption
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlSelect
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.JenkinsRule.WebClient
import spock.lang.Specification

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import static org.sonatype.nexus.ci.iq.IqServerMockUtility.configureIqServerMock
import static org.sonatype.nexus.ci.iq.IqServerMockUtility.configureJenkins
import static org.sonatype.nexus.ci.iq.Messages.IqPolicyEvaluation_AddIqServers
import static org.sonatype.nexus.ci.iq.Messages.IqPolicyEvaluation_Application
import static org.sonatype.nexus.ci.iq.Messages.IqPolicyEvaluation_ManualApplication
import static org.sonatype.nexus.ci.iq.Messages.IqPolicyEvaluation_NoIqServersConfigured
import static org.sonatype.nexus.ci.iq.Messages.IqPolicyEvaluation_SelectApplication
import static org.sonatype.nexus.ci.iq.Messages.IqPolicyEvaluation_Stage

/**
 * Functional test of the Pipeline syntax configuration page used for snippet generation.
 */
class PipelineSyntaxIntegrationTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort(), false)

  private final static String EVAL_OPTION = 'nexusPolicyEvaluation: Invoke Nexus Policy Evaluation'

  private static final int WAIT = 5000

  def 'Pipeline syntax page should load successfully when no IQ Server configured'() {
    given: 'a pipleline project'
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      WebClient client = jenkins.createWebClient()
      client.getOptions().setFetchPolyfillEnabled(true)

    when: 'the pipeline syntax page is loaded'
      HtmlOption policyEval = selectPolicyEvaluation(client, project)

    then: 'the sample step is present but not selected'
      policyEval
      !policyEval.selected

    when: 'we select the same step'
      HtmlPage clicked = policyEval.click()
      client.waitForBackgroundJavaScript(WAIT)

    then: 'the step is selected'
      policyEval.selected

    and: 'we are directed to configure an IQ server'
      String text = clicked.getVisibleText()
      text.contains(IqPolicyEvaluation_NoIqServersConfigured())
      text.contains(IqPolicyEvaluation_AddIqServers())
  }

  def 'Pipeline syntax page should load successfully when IQ Server configured'() {
    given: 'a pipleline project and configured IQ Server'
      configureIqServerMock(wireMockRule.port())
      configureJenkins(jenkins.jenkins, wireMockRule.port())
      WorkflowJob project = jenkins.createProject(WorkflowJob)
      WebClient client = jenkins.createWebClient()
      client.getOptions().setFetchPolyfillEnabled(true)

    when: 'the pipeline syntax page is loaded'
      HtmlOption policyEval = selectPolicyEvaluation(client, project)

    then: 'the sample step is present but not selected'
      policyEval
      !policyEval.selected

    when: 'we select the same step'
      HtmlPage clicked = policyEval.click()
      client.waitForBackgroundJavaScript(WAIT)
      String text = clicked.getVisibleText()
    
    then: 'the step is selected'
      policyEval.selected

    and: 'we are presented with a choice of Stage'
      text.contains(IqPolicyEvaluation_Stage())
      clicked.getFirstByXPath('//select[@name="_.iqStage"]')
    
    and: 'we are offered two ways to select an Application'
      text.contains(IqPolicyEvaluation_Application())
      clicked.getFirstByXPath("//label[contains(text(),'${IqPolicyEvaluation_ManualApplication()}')]")
      clicked.getFirstByXPath("//label[contains(text(),'${IqPolicyEvaluation_SelectApplication()}')]")

    and: 'advanced properties are available in the page (if not always visible)'
      String xml = clicked.asXml()
      xml.contains(Messages.IqPolicyEvaluation_ScanPatterns())    
      xml.contains(Messages.IqPolicyEvaluation_ModuleExcludes())    
      xml.contains(Messages.IqPolicyEvaluation_FailOnNetwork())    
      xml.contains(Messages.IqPolicyEvaluation_JobSpecificCredentials())    
      xml.contains('name="_.enableDebugLogging" type="checkbox"')
      xml.contains(Messages.IqPolicyEvaluation_AdvancedProperties())    
  }

  private HtmlOption selectPolicyEvaluation(WebClient client, WorkflowJob project) {
    def pipelineSyntax = client.getPage(project, 'pipeline-syntax')
    HtmlSelect select = pipelineSyntax.getFirstByXPath('//select')
    HtmlOption policyEval = select.getOptionByValue(EVAL_OPTION)
    return policyEval
  }
}
