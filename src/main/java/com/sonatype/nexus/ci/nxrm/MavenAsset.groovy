/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm

import hudson.Extension
import org.jenkinsci.Symbol
import org.kohsuke.stapler.DataBoundConstructor

@Symbol('mavenAsset')
class MavenAsset
    extends Asset
{
  final String filePath

  final String classifier

  final String extension

  @DataBoundConstructor
  MavenAsset(final String filePath, final String classifier, final String extension) {
    this.filePath = filePath
    this.classifier = classifier
    this.extension = extension
  }

  @Extension
  public static final class DescriptorImpl
      extends Asset.AssetDescriptor<MavenAsset>
  {
    @Override
    public String getDisplayName() {
      return 'Maven Artifact'
    }
  }
}
