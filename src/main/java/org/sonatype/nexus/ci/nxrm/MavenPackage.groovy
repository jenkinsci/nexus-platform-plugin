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
  static final class DescriptorImpl
      extends Package.PackageDescriptor
  {
    DescriptorImpl() {
      super(MavenPackage)
    }

    @Override
    String getDisplayName() {
      return 'Maven Package'
    }
  }
}
