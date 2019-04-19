/*
 * Copyright (c) 2019-present Sonatype, Inc. All rights reserved.
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
package org.sonatype.nexus.ci.nvs

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.NxiqConfiguration

import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

class NvsMessageUtilTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  def 'showMessage returns true by default'() {
    expect:
      NvsMessageUtil.showMessage()
  }

  def 'showMessage returns false when IQ is configured'() {
    setup:
      def iqConfig = new NxiqConfiguration("", "")
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = [iqConfig]
      globalConfiguration.save()
    expect:
      !NvsMessageUtil.showMessage()
  }

  def 'showMessage returns false when checkbox to hide NVS message is checked'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.hideNvsMessage = true
      globalConfiguration.save()
    expect:
      !NvsMessageUtil.showMessage()
  }

  def 'showMessage returns false when IQ and checkbox are both set'() {
    setup:
      def iqConfig = new NxiqConfiguration("", "")
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      globalConfiguration.iqConfigs = [iqConfig]
      globalConfiguration.hideNvsMessage = true
      globalConfiguration.save()
    expect:
      !NvsMessageUtil.showMessage()
  }
}
