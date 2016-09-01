/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm

import hudson.Extension
import org.jenkinsci.Symbol
import org.kohsuke.stapler.DataBoundConstructor

@Symbol('mavenPackage')
class MavenPackage
    extends Package
{
  final MavenCoordinate coordinate

  final List<MavenAsset> assets

  /**
   * Hudson uses reflection to determine which Descriptors to associate with bound fields. These are stored in a map
   * such that covariance allows the super class to override the Descriptor for a field. Use a different getter
   * and constructor for view binding.
   */
  MavenCoordinate getMavenCoordinate() {
    return coordinate
  }

  /**
   * Hudson uses reflection to determine which Descriptors to associate with bound fields. These are stored in a map
   * such that covariance allows the super class to override the Descriptor for a field. Use a different getter
   * and constructor for view binding.
   */
  List<MavenAsset> getMavenAssetList() {
    return assets
  }

  @DataBoundConstructor
  MavenPackage(final MavenCoordinate mavenCoordinate, final List<MavenAsset> mavenAssetList) {
    this.coordinate = mavenCoordinate
    this.assets = mavenAssetList ?: []
  }

  @Extension
  public static final class DescriptorImpl
      extends Package.PackageDescriptor
  {
    public DescriptorImpl() {
      super(MavenPackage.class)
    }

    @Override
    public String getDisplayName() {
      return 'Maven Package'
    }
  }
}
