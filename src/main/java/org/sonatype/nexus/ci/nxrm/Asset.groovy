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
package org.sonatype.nexus.ci.nxrm

import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor

@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class Asset
    extends AbstractDescribableImpl<Asset>
{
  /**
   * Assets are selected from the specific {@link Asset} applicable to a
   * {@link Package} type so the generic should be typed to the
   * {@link Asset} implementation in order to populate the
   * {@link lib.FormTagLib#repeatableHeteroProperty(java.util.Map, groovy.lang.Closure)}
   *
   * @param <T> The Asset implementation that will be available from the view's
   * {@link lib.FormTagLib#repeatableHeteroProperty(java.util.Map, groovy.lang.Closure)}
   */
  static abstract class AssetDescriptor<T extends Asset>
      extends Descriptor<T>
  {

  }
}
