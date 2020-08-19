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

import hudson.Extension
import hudson.model.Descriptor
import jenkins.model.GlobalConfiguration
import net.sf.json.JSONObject
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.StaplerRequest

@Extension
class GlobalNexusConfiguration
    extends GlobalConfiguration
{
  List<NxrmConfiguration> nxrmConfigs

  List<NxiqConfiguration> iqConfigs

  String instanceId

  GlobalNexusConfiguration() {
    load()
    if (!instanceId) {
      instanceId = generateInstanceId()
      save()
    }
  }

  @DataBoundConstructor
  GlobalNexusConfiguration(final List<NxrmConfiguration> nxrmConfigs, final List<NxiqConfiguration> iqConfigs) {
    this.nxrmConfigs = nxrmConfigs ?: []
    this.iqConfigs = iqConfigs ?: []
  }

  @Override
  boolean configure(final StaplerRequest req, final JSONObject json) throws Descriptor.FormException {
    def globalConfiguration = req.bindJSON(GlobalNexusConfiguration, json)
    this.nxrmConfigs = globalConfiguration.nxrmConfigs
    this.iqConfigs = globalConfiguration.iqConfigs
    save()
    return true
  }

  @Override
  String getDisplayName() {
    return 'Sonatype Nexus'
  }

  static getGlobalNexusConfiguration() {
    return all().get(GlobalNexusConfiguration)
  }

  static String getInstanceId() {
    getGlobalNexusConfiguration()?.@instanceId
  }

  private generateInstanceId() {
    UUID.randomUUID().toString().replace('-', '')
  }
}
