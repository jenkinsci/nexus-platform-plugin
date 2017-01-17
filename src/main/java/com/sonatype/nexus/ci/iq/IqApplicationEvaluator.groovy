/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import com.sonatype.nexus.api.iq.IqClient
import com.sonatype.nexus.api.iq.PolicyEvaluationResult

import hudson.FilePath
import org.codehaus.plexus.util.DirectoryScanner

class IqApplicationEvaluator
{
  private final IqClient iqClient

  private final DirectoryScanner directoryScanner

  IqApplicationEvaluator(final IqClient iqClient, final DirectoryScanner directoryScanner) {
    this.directoryScanner = directoryScanner
    this.iqClient = iqClient
  }

  PolicyEvaluationResult performScan(final String appId, final String stageId, final List<String> patterns,
                                     final FilePath workspace)
  {
    Properties config = new Properties()
    def targets = getTargets(new File(workspace.getRemote()), patterns)
    return iqClient.evaluateApplication(appId, config, targets, stageId)
  }

  private List<File> getTargets(final File workDir, final List<String> patterns) {
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
