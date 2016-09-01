/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm

import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor

abstract class Package
    extends AbstractDescribableImpl<Package>
{
  Coordinate coordinate

  List<? extends Asset> assets

  /**
   * Currently Packages are selected from all extended {@link hudson.model.Descriptor} so generic type should be
   * {@link com.sonatype.nexus.ci.nxrm.Package} in order to build the view's
   * {@link lib.FormTagLib#repeatableHeteroProperty(java.util.Map, groovy.lang.Closure)}
   */
  static abstract class PackageDescriptor
      extends Descriptor<Package>
  {
    public PackageDescriptor(Class<? extends Package> clazz) {
      super(clazz)
    }
  }
}
