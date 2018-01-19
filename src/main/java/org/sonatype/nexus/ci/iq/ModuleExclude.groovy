package org.sonatype.nexus.ci.iq

import hudson.Extension
import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor
import org.kohsuke.stapler.DataBoundConstructor

class ModuleExclude
    extends AbstractDescribableImpl<ModuleExclude>
{
  String moduleExclude

  @DataBoundConstructor
  ModuleExclude(String moduleExclude) {
    this.moduleExclude = moduleExclude
  }

  @Extension
  static class DescriptorImpl
      extends Descriptor<ModuleExclude>
  {
    @Override
    String getDisplayName() {
      return Messages.ModuleExclude_DisplayName()
    }
  }
}
