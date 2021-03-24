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

  def 'Pipeline syntax page should load successfully'() {
    given: 'a pipleline project'
      WorkflowJob project = jenkins.createProject(WorkflowJob)

    when: 'the pipeline syntax page is loaded'
      def pipelineSyntax = jenkins.createWebClient().getPage(project, 'pipeline-syntax')

    then: 'the sample step is present'
      pipelineSyntax.getFirstByXPath('//select/option[@value=\'nexusPolicyEvaluation: Invoke Nexus Policy Evaluation\']')
  }
}
