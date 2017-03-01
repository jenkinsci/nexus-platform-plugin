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
import org.slf4j.Logger

class RemoteScanner
    extends MasterToSlaveCallable<RemoteScanResult, RuntimeException>
{
  private final String appId

  private final String stageId

  private final List<String> patterns

  private final FilePath workspace

  private final ProprietaryConfig proprietaryConfig

  private final Logger log

  private final String instanceId

  @SuppressWarnings('ParameterCount')
  RemoteScanner(final String appId,
                final String stageId,
                final List<String> patterns,
                final FilePath workspace,
                final ProprietaryConfig proprietaryConfig,
                final Logger log,
                final String instanceId)
  {
    this.appId = appId
    this.stageId = stageId
    this.patterns = patterns
    this.workspace = workspace
    this.proprietaryConfig = proprietaryConfig
    this.log = log
    this.instanceId = instanceId
  }

  @Override
  RemoteScanResult call() throws RuntimeException {
    InternalIqClient iqClient = IqClientFactory.getIqLocalClient(log, instanceId)
    def targets = getTargets(new File(workspace.getRemote()), patterns)
    def scanResult = iqClient.scan(appId, proprietaryConfig, new Properties(), targets)
    return new RemoteScanResult(scanResult.scan, new FilePath(scanResult.scanFile))
  }

  private List<File> getTargets(final File workDir, final List<String> patterns) {
    def directoryScanner = RemoteScannerFactory.getDirectoryScanner()
    directoryScanner.setBasedir(workDir)
    directoryScanner.setIncludes(patterns.toArray(new String[patterns.size()]))
    directoryScanner.addDefaultExcludes()
    directoryScanner.scan()
    return (directoryScanner.getIncludedDirectories() + directoryScanner.getIncludedFiles())
        .collect { f -> new File(workDir, f) }
        .sort()
        .asImmutable()
  }
}
