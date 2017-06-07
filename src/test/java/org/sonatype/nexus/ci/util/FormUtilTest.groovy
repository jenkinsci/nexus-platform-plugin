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
package org.sonatype.nexus.ci.util

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.NxiqConfiguration

import hudson.util.FormValidation.Kind
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

class FormUtilTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  def 'calls IqUtil with the correct arguments'() {
    setup:
      GroovyMock(IqUtil, global: true)
      final String serverUrl = 'http://localhost/'
      final String globalCredentialsId = 'globalCredentialsId'

      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxiqConfiguration = new NxiqConfiguration(serverUrl, globalCredentialsId)
      globalConfiguration.iqConfigs = []
      globalConfiguration.iqConfigs.add(nxiqConfiguration)
      globalConfiguration.save()

    when:
      def validation = FormUtil.validateJobCredentials(creds, jenkins.instance)

    then:
      1 * IqUtil.getApplicableApplications(serverUrl, credentialsId, jenkins.instance) >> []
      validation.kind == Kind.OK

    where:
      creds       | credentialsId
      'creds-123' | 'creds-123'
      ''          | 'globalCredentialsId'
      null        | 'globalCredentialsId'
  }
}
