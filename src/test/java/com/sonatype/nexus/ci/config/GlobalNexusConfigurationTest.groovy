/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.config

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
    nxiqConfiguration = [new NxiqConfiguration('http://server/url', false, 'creds-id')]
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

  @WithoutJenkins
  def 'DataBoundConstructor initialises fields'() {
    when:
      def globalNexusConfiguration = new GlobalNexusConfiguration(nxrmConfiguration, nxiqConfiguration)

    then:
      globalNexusConfiguration.iqConfigs == nxiqConfiguration
      globalNexusConfiguration.nxrmConfigs == nxrmConfiguration
  }
}
