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

@SuppressWarnings(['AbcMetric'])
class ScanPatternUtil
{
  static final List<String> DEFAULT_SCAN_PATTERN =
      ['**/*.jar', '**/*.war', '**/*.ear', '**/*.zip', '**/*.tar.gz']

  public static final String EXCLUDE_MARKER = '!'

  // Warning: Only use this in the context of a MasterToSlaveCallable
  static List<File> getScanTargets(final File workDir, final List<String> scanPatterns) {
    def directoryScanner = RemoteScannerFactory.getDirectoryScanner()
    def normalizedScanPatterns = scanPatterns ?: DEFAULT_SCAN_PATTERN
    def includeScanPatterns = normalizedScanPatterns.findAll{!it.startsWith(EXCLUDE_MARKER)}
    def excludeScanPatterns = normalizedScanPatterns.findAll{it.startsWith(EXCLUDE_MARKER)}.collect{it.substring(1)}
    directoryScanner.setBasedir(workDir)
    directoryScanner.setIncludes(includeScanPatterns.toArray(new String[includeScanPatterns.size()]))
    directoryScanner.setExcludes(excludeScanPatterns.toArray(new String[excludeScanPatterns.size()]))
    directoryScanner.addDefaultExcludes()
    directoryScanner.scan()
    return (directoryScanner.getIncludedDirectories() + directoryScanner.getIncludedFiles())
        .collect { f -> new File(workDir, f) }
        .sort()
        .asImmutable()
  }
}
