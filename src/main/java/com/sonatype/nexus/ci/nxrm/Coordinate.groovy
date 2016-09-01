/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm

import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor

abstract class Coordinate
    extends AbstractDescribableImpl<Coordinate>
{
  static abstract class CoordinateDescriptor<T extends Coordinate>
      extends Descriptor<T>
  {

  }
}
