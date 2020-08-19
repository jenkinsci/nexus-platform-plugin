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

import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter
import hudson.model.Describable
import hudson.model.Descriptor
import hudson.util.FormValidation
import jenkins.model.Jenkins
import org.kohsuke.stapler.QueryParameter

@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class IqApplication
    implements Describable<IqApplication>
{
  String applicationId

  protected IqApplication(final String applicationId) {
    this.applicationId = applicationId
  }

  @Override
  Descriptor<IqApplication> getDescriptor() {
    return Jenkins.getInstance().getDescriptorOrDie(this.getClass())
  }

  /**
   * Provide Migration path from String to IqApplication
   */
  static final class ConverterImpl
      extends AbstractSingleValueConverter
  {
    boolean canConvert(Class clazz) {
      return clazz == IqApplication.class
    }

    Object fromString(String str) {
      return new SelectedApplication(str)
    }
  }

  static class IqApplicationDescriptor
      extends Descriptor<IqApplication>
  {
    IqApplicationDescriptor(Class<? extends IqApplication> clazz) {
      super(clazz)
    }

    FormValidation doCheckApplicationId(@QueryParameter String value) {
      return FormValidation.validateRequired(value)
    }
  }
}
