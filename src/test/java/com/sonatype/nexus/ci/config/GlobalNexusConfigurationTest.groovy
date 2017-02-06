/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.config

import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

class GlobalNexusConfigurationTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

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
}
