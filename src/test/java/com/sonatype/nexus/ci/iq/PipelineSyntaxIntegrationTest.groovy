/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

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
