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
package org.sonatype.nexus.ci.nxrm.v3;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import static com.sonatype.nexus.api.common.ArgumentUtils.checkArgument;
import static com.sonatype.nexus.api.common.NexusStringUtils.isNotBlank;
import static org.sonatype.nexus.ci.nxrm.Messages.SearchParameter_DisplayName;
import static org.sonatype.nexus.ci.nxrm.Messages.SearchParameter_Validation_KeyRequired;
import static org.sonatype.nexus.ci.nxrm.Messages.SearchParameter_Validation_ValueRequired;
import static org.sonatype.nexus.ci.util.FormUtil.validateNotEmpty;

public class SearchParameter
    extends AbstractDescribableImpl<SearchParameter>
{
  private final String key;

  private final String value;

  @DataBoundConstructor
  public SearchParameter(final String key, final String value) {
    this.key = checkArgument(key, isNotBlank(key), SearchParameter_Validation_KeyRequired());
    this.value = checkArgument(value, isNotBlank(value), SearchParameter_Validation_ValueRequired());
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }

  @Extension
  public static class DescriptorImpl
      extends Descriptor<SearchParameter>
  {
    @Override
    public String getDisplayName() {
      return SearchParameter_DisplayName();
    }

    public FormValidation doCheckKey(@QueryParameter String key) {
      return validateNotEmpty(key, SearchParameter_Validation_KeyRequired());
    }

    public FormValidation doCheckValue(@QueryParameter String value) {
      return validateNotEmpty(value, SearchParameter_Validation_ValueRequired());
    }
  }
}

