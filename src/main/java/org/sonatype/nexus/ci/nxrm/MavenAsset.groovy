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

import groovy.transform.ToString
import hudson.Extension
import org.jenkinsci.Symbol
import org.kohsuke.stapler.DataBoundConstructor

@ToString(includeNames = true, includePackage = false)
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
  static final class DescriptorImpl
      extends Asset.AssetDescriptor<MavenAsset>
  {
    @Override
    String getDisplayName() {
      return 'Maven Artifact'
    }
  }
}
