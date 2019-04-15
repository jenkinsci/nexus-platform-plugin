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

import com.sonatype.nexus.api.iq.ProprietaryConfig
import com.sonatype.nexus.api.iq.internal.InternalIqClient

import hudson.FilePath
import jenkins.security.MasterToSlaveCallable
import org.codehaus.plexus.util.DirectoryScanner
import org.slf4j.Logger

class RemoteScanner
    extends MasterToSlaveCallable<RemoteScanResult, RuntimeException>
{
  static final List<String> DEFAULT_SCAN_PATTERN =
      ['**/*.jar', '**/*.war', '**/*.ear', '**/*.zip', '**/*.tar.gz']

  static final List<String> DEFAULT_MODULE_INCLUDES =
      ['**/sonatype-clm/module.xml', '**/nexus-iq/module.xml']

  private final String appId

  private final String stageId

  private final List<String> scanPatterns

  private final List<String> moduleExcludes

  private final FilePath workspace

  private final ProprietaryConfig proprietaryConfig

  private final Logger log

  private final String instanceId

  private final Properties advancedProperties

  @SuppressWarnings('ParameterCount')
  RemoteScanner(final String appId,
                final String stageId,
                final List<String> scanPatterns,
                final List<String> moduleExcludes,
                final FilePath workspace,
                final ProprietaryConfig proprietaryConfig,
                final Logger log,
                final String instanceId,
                final Properties advancedProperties)
  {
    this.appId = appId
    this.stageId = stageId
    this.scanPatterns = scanPatterns
    this.moduleExcludes = moduleExcludes
    this.workspace = workspace
    this.proprietaryConfig = proprietaryConfig
    this.log = log
    this.instanceId = instanceId
    this.advancedProperties = advancedProperties
  }

  @Override
  RemoteScanResult call() throws RuntimeException {
    InternalIqClient iqClient = IqClientFactory.getIqLocalClient(log, instanceId)
    def workDirectory = new File(workspace.getRemote())
    def targets = getScanTargets(workDirectory, scanPatterns)
    def moduleIndices = getModuleIndices(workDirectory, moduleExcludes)
    def scanResult = iqClient.scan(appId, proprietaryConfig, advancedProperties, targets, moduleIndices, workDirectory)
    return new RemoteScanResult(scanResult.scan, new FilePath(scanResult.scanFile))
  }

  List<File> getScanTargets(final File workDir, final List<String> scanPatterns) {
    def directoryScanner = RemoteScannerFactory.getDirectoryScanner()
    def normalizedScanPatterns = scanPatterns ?: DEFAULT_SCAN_PATTERN
    directoryScanner.setBasedir(workDir)
    directoryScanner.setIncludes(normalizedScanPatterns.toArray(new String[normalizedScanPatterns.size()]))
    directoryScanner.addDefaultExcludes()
    directoryScanner.scan()
    return (directoryScanner.getIncludedDirectories() + directoryScanner.getIncludedFiles())
        .collect { f -> new File(workDir, f) }
        .sort()
        .asImmutable()
  }

  List<File> getModuleIndices(final File workDirectory, final List<String> moduleExcludes) {
    final DirectoryScanner directoryScanner = RemoteScannerFactory.getDirectoryScanner()
    directoryScanner.setBasedir(workDirectory)
    directoryScanner.setIncludes(DEFAULT_MODULE_INCLUDES.toArray(new String[DEFAULT_MODULE_INCLUDES.size()]))
    if (moduleExcludes) {
      directoryScanner.setExcludes(moduleExcludes.toArray(new String[moduleExcludes.size()]))
    }
    directoryScanner.addDefaultExcludes()
    directoryScanner.scan()
    return directoryScanner.getIncludedFiles()
        .collect { f -> new File(workDirectory, f) }
        .sort()
        .asImmutable()
  }
}
