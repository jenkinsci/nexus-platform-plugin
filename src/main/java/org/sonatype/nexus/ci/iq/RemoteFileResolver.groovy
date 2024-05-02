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
 package org.sonatype.nexus.ci.iq

import jenkins.security.MasterToSlaveCallable

class RemoteFileResolver
    extends MasterToSlaveCallable<List<String>, RuntimeException>
{
  private final File workDir;
  private final List<String> scanPatterns;

  RemoteFileResolver(final File workDir,final List<String> scanPatterns) {
    this.workDir = workDir
    this.scanPatterns = scanPatterns
  }

  @Override
  List<String> call() throws RuntimeException {
    return ScanPatternUtil.getScanTargets(workDir, scanPatterns)
        .collect {
          return it.getAbsolutePath()
        }
  }
}
