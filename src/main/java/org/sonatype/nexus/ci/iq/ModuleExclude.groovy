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
