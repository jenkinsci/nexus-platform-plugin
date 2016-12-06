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

  List<NxiqConfiguration> iqConfigs

  GlobalNexusConfiguration() {
    load()
  }

  @DataBoundConstructor
  GlobalNexusConfiguration(final List<NxrmConfiguration> nxrmConfigs, final List<NxiqConfiguration> iqConfigs) {
    this.nxrmConfigs = nxrmConfigs ?: []
    this.iqConfigs = iqConfigs ?: []
  }

  @Override
  boolean configure(StaplerRequest req, JSONObject json) throws Descriptor.FormException {
    this.nxrmConfigs = json.get(nxrmConfigs) as List ?: []
    this.iqConfigs = json.get(iqConfigs) as List ?: []
    save()
    return true
  }

  @Override
  String getDisplayName() {
    return 'Sonatype Nexus'
  }
}
