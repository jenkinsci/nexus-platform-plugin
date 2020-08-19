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
package org.sonatype.nexus.ci.nxrm

import java.lang.reflect.Type

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.Nxrm2Configuration
import org.sonatype.nexus.ci.config.Nxrm3Configuration
import org.sonatype.nexus.ci.config.NxrmConfiguration
import org.sonatype.nexus.ci.nxrm.v2.ComponentUploaderNxrm2
import org.sonatype.nexus.ci.nxrm.v3.ComponentUploaderNxrm3

import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification
import spock.lang.Unroll

class ComponentUploaderFactoryTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  def run = Mock(Run)

  def taskListener = Mock(TaskListener)

  def 'it fails the build if Nexus configuration not available'() {
    setup:
      def nxrmConfiguration = new Nxrm2Configuration('id', 'internalId', 'displayName', 'http://localhost', 'credId')
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.nxrmConfigs = []
      globalConfiguration.nxrmConfigs.add(nxrmConfiguration)
      globalConfiguration.save()

    when:
      ComponentUploaderFactory.getComponentUploader("other", run, taskListener)

    then:
      1 * run.setResult(Result.FAILURE)
      def ex = thrown(IllegalArgumentException)
      ex.message == 'Nexus Configuration other not found.'
  }

  def 'it fails the build if Nexus server uri is not valid'() {
    setup:
      def nxrmConfiguration = new Nxrm2Configuration('id', 'internalId', 'displayName', 'foo', 'credId')
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.nxrmConfigs = []
      globalConfiguration.nxrmConfigs.add(nxrmConfiguration)
      globalConfiguration.save()

    when:
      ComponentUploaderFactory.getComponentUploader('id', run, taskListener)

    then:
      1 * run.setResult(Result.FAILURE)
      def ex = thrown(IllegalArgumentException)
      ex.message == 'Nexus Server URL foo is invalid.'
  }

  @Unroll
  def 'it creates uploader for #nxrmVersion'(String nxrmVersion,
                                             NxrmConfiguration config,
                                             Type expectedType)
  {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.nxrmConfigs = []
      globalConfiguration.nxrmConfigs.add(config)
      globalConfiguration.save()

    when:
      def uploader = ComponentUploaderFactory.getComponentUploader('id', run, taskListener)

    then:
      uploader.class == expectedType

    where:
      nxrmVersion << ['NXRM 2', 'NXRM 3']
      config << [new Nxrm2Configuration('id', 'internalId', 'displayName', 'http://localhost', 'credId'),
                 new Nxrm3Configuration('id', 'internalId', 'displayName', 'http://localhost', 'credId')]
      expectedType << [ComponentUploaderNxrm2, ComponentUploaderNxrm3]
  }
}
