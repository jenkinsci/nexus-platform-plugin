/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm

import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor

@SuppressWarnings('AbstractClassWithoutAbstractMethod') // TODO ignored for existing code. refactor when convenient
abstract class Asset
    extends AbstractDescribableImpl<Asset>
{
  /**
   * Assets are selected from the specific {@link com.sonatype.nexus.ci.nxrm.Asset} applicable to a
   * {@link com.sonatype.nexus.ci.nxrm.Package} type so the generic should be typed to the
   * {@link com.sonatype.nexus.ci.nxrm.Asset} implementation in order to populate the
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
