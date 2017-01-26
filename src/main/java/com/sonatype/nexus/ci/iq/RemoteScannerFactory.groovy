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
  static RemoteScanner getRemoteScanner(final String appId,
                                        final String stageId,
                                        final List<String> patterns,
                                        final FilePath workspace,
                                        final URI iqServerUrl,
                                        final ProprietaryConfig proprietaryConfig,
                                        final Logger log)
  {
    new RemoteScanner(appId, stageId, patterns, workspace, iqServerUrl, proprietaryConfig, log)
  }

  static DirectoryScanner getDirectoryScanner() {
    new DirectoryScanner()
  }
}
