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
package org.sonatype.nexus.ci.config

import com.sonatype.insight.client.utils.InstanceIdProvider
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.WithoutJenkins
import spock.lang.Specification

class GlobalNexusConfigurationTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  List<? extends NxrmConfiguration> nxrmConfiguration
  List<NxiqConfiguration> nxiqConfiguration

  void setup() {
    nxrmConfiguration = [new Nxrm2Configuration('id', 'int-id', 'display-name', 'http://server/url', 'creds-id')]
    nxiqConfiguration = [new NxiqConfiguration('id', 'int-id', 'display-name', 'http://server/url', 'creds-id', false)]
  }

  def 'new instance ID is generated and loaded from configuration file'() {
    setup:
      GlobalNexusConfiguration globalNexusConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      String uuid1 = globalNexusConfiguration.instanceId

    when:
      globalNexusConfiguration.instanceId = null
      globalNexusConfiguration.load()
      String uuid2 = globalNexusConfiguration.instanceId

    then:
      uuid1 == uuid2
  }

  def 'InstanceIdProvider#instanceId is set to Configuration#instanceId'() {
    setup:
    GlobalNexusConfiguration globalNexusConfiguration = GlobalNexusConfiguration.globalNexusConfiguration

    when:
    globalNexusConfiguration.load()

    then:
    globalNexusConfiguration.instanceId == InstanceIdProvider.instanceId
  }

  @WithoutJenkins
  def 'DataBoundConstructor initialises fields'() {
    when:
      def globalNexusConfiguration = new GlobalNexusConfiguration(nxrmConfiguration, nxiqConfiguration)

    then:
      globalNexusConfiguration.iqConfigs == nxiqConfiguration
      globalNexusConfiguration.nxrmConfigs == nxrmConfiguration
  }
}
