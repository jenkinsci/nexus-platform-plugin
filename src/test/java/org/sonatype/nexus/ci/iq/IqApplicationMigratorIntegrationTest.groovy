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

import com.sonatype.nexus.api.iq.internal.InternalIqClientBuilder

import hudson.model.FreeStyleProject
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

class IqApplicationMigratorIntegrationTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule().withExistingHome(new File(
      getClass().getClassLoader().getResource('org/sonatype/nexus/ci/config/ComToOrgMigratorIntegrationTest').
          getFile()))

  def setup() {
    GroovyMock(InternalIqClientBuilder, global: true)
    def iqClientBuilder = Mock(InternalIqClientBuilder)
    InternalIqClientBuilder.create() >> iqClientBuilder

    iqClientBuilder.withProxyConfig(_) >> iqClientBuilder
    iqClientBuilder.withServerConfig(_) >> iqClientBuilder
    iqClientBuilder.withLogger(_) >> iqClientBuilder
    iqClientBuilder.withInstanceId(_) >> iqClientBuilder
  }

  def 'it migrates a Freestyle IQ job'() {
    when:
      def project = (FreeStyleProject)jenkins.jenkins.getItem('Freestyle-IQ')
      def buildStep = (IqPolicyEvaluatorBuildStep)project.builders[0]

    then: 'the application is scanned and evaluated'
      buildStep.iqStage == 'build'
      buildStep.iqApplication.applicationId == 'sample-app'
      buildStep.failBuildOnNetworkError
      buildStep.jobCredentialsId == 'user2'
      buildStep.iqScanPatterns.size() == 1
      buildStep.iqScanPatterns[0].scanPattern == 'target/*.jar'
  }
}
