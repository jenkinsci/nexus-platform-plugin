/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import hudson.Extension
import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor
import org.kohsuke.stapler.DataBoundConstructor

class ScanPattern
    extends AbstractDescribableImpl<ScanPattern>
{
  public String scanPattern

  @DataBoundConstructor
  ScanPattern(String scanPattern) {
    this.scanPattern = scanPattern
  }

  @Extension
  static class DescriptorImpl
      extends Descriptor<ScanPattern>
  {
    @Override
    String getDisplayName() {
      return "Scan pattern"
    }
  }
}
