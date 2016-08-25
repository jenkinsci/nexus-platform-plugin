/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.config

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

  GlobalNexusConfiguration() {
    load()
  }

  @DataBoundConstructor
  GlobalNexusConfiguration(final List<NxrmConfiguration> nxrmConfigs) {
    this.nxrmConfigs = nxrmConfigs ?: new ArrayList<>()
  }

  @Override
  public boolean configure(StaplerRequest req, JSONObject json) throws Descriptor.FormException {
    // HACK to fix empty JSON response. Bind JSON iterates through available properties in the JSON and sets them.
    // If list is empty, doesn't show up in JSON and is not iterated through
    def globalConfiguration = req.bindJSON(GlobalNexusConfiguration.class, json)
    this.nxrmConfigs = globalConfiguration.nxrmConfigs
    save()
    return true
  }

  @Override
  public String getDisplayName() {
    return 'Sonatype Nexus'
  }
}
