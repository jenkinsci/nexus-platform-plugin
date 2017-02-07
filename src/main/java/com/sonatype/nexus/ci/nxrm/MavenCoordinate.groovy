/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm

import hudson.Extension
import org.jenkinsci.Symbol
import org.kohsuke.stapler.DataBoundConstructor

@Symbol('mavenCoordinate')
class MavenCoordinate
    extends Coordinate
{
  final String groupId

  final String artifactId

  final String version

  final String packaging

  @DataBoundConstructor
  MavenCoordinate(final String groupId, final String artifactId, final String version, final String packaging) {
    this.groupId = groupId
    this.artifactId = artifactId
    this.version = version
    this.packaging = packaging
  }

  @Extension
  static final class DescriptorImpl
      extends Coordinate.CoordinateDescriptor<MavenCoordinate>
  {
    @Override
    String getDisplayName() {
      return 'Maven Coordinate'
    }
  }
}
