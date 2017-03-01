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

import hudson.model.Describable
import hudson.model.Descriptor
import jenkins.model.Jenkins

@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class NxrmConfiguration
    implements Describable<NxrmConfiguration>
{
  String id

  /**
   * Used as a unique identifier per instance to ensure unique Display Name and Id
   */
  String internalId

  String displayName

  String serverUrl

  String credentialsId

  @Override
  Descriptor<NxrmConfiguration> getDescriptor() {
    return Jenkins.getInstance().getDescriptorOrDie(this.getClass())
  }

  /**
   * Currently NxrmConfigurations are selected from all extended {@link hudson.model.Descriptor} so generic type should
   * be {@link NxrmConfiguration} in order to build the view's
   * {@link lib.FormTagLib#repeatableHeteroProperty(java.util.Map, groovy.lang.Closure)}
   */
  static abstract class NxrmDescriptor
      extends Descriptor<NxrmConfiguration>
  {
    @SuppressWarnings('AbstractClassWithPublicConstructor')
    NxrmDescriptor(Class<? extends NxrmConfiguration> clazz) {
      super(clazz)
    }
  }
}
