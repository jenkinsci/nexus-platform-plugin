/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

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

  private final URI iqServerUrl

  private final ProprietaryConfig proprietaryConfig

  private final Logger log

  private final String instanceId

  @SuppressWarnings('ParameterCount') // TODO ignore warning for existing code, refactor when convenient
  RemoteScanner(final String appId,
                final String stageId,
                final List<String> patterns,
                final FilePath workspace,
                final URI iqServerUrl,
                final ProprietaryConfig proprietaryConfig,
                final Logger log,
                final String instanceId)
  {
    this.appId = appId
    this.stageId = stageId
    this.patterns = patterns
    this.workspace = workspace
    this.iqServerUrl = iqServerUrl
    this.proprietaryConfig = proprietaryConfig
    this.log = log
    this.instanceId = instanceId
  }

  @Override
  RemoteScanResult call() throws RuntimeException {
    InternalIqClient iqClient = IqClientFactory.getIqClient(iqServerUrl, log, instanceId)
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
