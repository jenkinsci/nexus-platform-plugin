package org.sonatype.nexus.ci.nxrm.v3

import org.sonatype.nexus.ci.nxrm.Messages

import hudson.Extension
import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor
import hudson.util.FormValidation
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

import static com.sonatype.nexus.api.common.ArgumentUtils.checkArgument
import static com.sonatype.nexus.api.common.NexusStringUtils.isNotBlank
import static org.sonatype.nexus.ci.nxrm.Messages.SearchCriteria_Validation_KeyRequired
import static org.sonatype.nexus.ci.nxrm.Messages.SearchCriteria_Validation_ValueRequired
import static org.sonatype.nexus.ci.util.FormUtil.validateNotEmpty

class SearchCriteria
    extends AbstractDescribableImpl<SearchCriteria>
{
  String key

  String value

  @DataBoundConstructor
  SearchCriteria(String key, String value) {
    this.key = checkArgument(key, isNotBlank(key), SearchCriteria_Validation_KeyRequired())
    this.value = checkArgument(value, isNotBlank(value), SearchCriteria_Validation_ValueRequired())
  }

  @Extension
  static class DescriptorImpl
      extends Descriptor<SearchCriteria>
  {
    @Override
    String getDisplayName() {
      return Messages.SearchCriteria_DisplayName()
    }

    FormValidation doCheckKey(@QueryParameter String key) {
      return validateNotEmpty(key, SearchCriteria_Validation_KeyRequired())
    }

    FormValidation doCheckValue(@QueryParameter String value) {
      return validateNotEmpty(value, SearchCriteria_Validation_ValueRequired())
    }
  }
}

