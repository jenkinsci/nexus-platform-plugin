/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import com.sonatype.nexus.api.iq.ProprietaryConfig

import hudson.FilePath
import org.codehaus.plexus.util.DirectoryScanner
import org.slf4j.Logger

class RemoteScannerFactory
{
  @SuppressWarnings('ParameterCount') // TODO ignore warning for existing code, refactor when convenient
  static RemoteScanner getRemoteScanner(final String appId,
                                        final String stageId,
                                        final List<String> patterns,
                                        final FilePath workspace,
                                        final ProprietaryConfig proprietaryConfig,
                                        final Logger log,
                                        final String instanceId)
  {
    new RemoteScanner(appId, stageId, patterns, workspace, proprietaryConfig, log, instanceId)
  }

  static DirectoryScanner getDirectoryScanner() {
    new DirectoryScanner()
  }
}
