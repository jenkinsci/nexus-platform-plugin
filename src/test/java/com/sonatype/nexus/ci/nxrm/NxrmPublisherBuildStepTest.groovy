/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm

import hudson.model.Describable

class NxrmPublisherBuildStepTest
    extends NxrmPublisherDescriptorTest
{
  @Override
  Class<? extends Describable> getDescribable() {
    return NxrmPublisherBuildStep.class
  }
}
