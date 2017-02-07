/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.config

import javax.annotation.Nullable

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

  static @Nullable getGlobalNexusConfiguration() {
    return all().get(GlobalNexusConfiguration)
  }

  static getInstanceId() {
    getGlobalNexusConfiguration()?.@instanceId
  }

  private generateInstanceId() {
    UUID.randomUUID().toString().replace('-', '')
  }
}
