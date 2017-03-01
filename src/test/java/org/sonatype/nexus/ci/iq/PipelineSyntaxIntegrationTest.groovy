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

import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

class PipelineSyntaxIntegrationTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  def 'Pipeline syntax page should load successfully'() {
    given: 'a pipleline project'
      WorkflowJob project = jenkins.createProject(WorkflowJob)

    when: 'the pipeline syntax page is loaded'
      def pipelineSyntax = jenkins.createWebClient().getPage(project, 'pipeline-syntax')

    then: 'the sample step is present'
      pipelineSyntax.getFirstByXPath('//select/option[@value=\'nexusPolicyEvaluation: Invoke Nexus Policy Evaluation\']')
  }
}
