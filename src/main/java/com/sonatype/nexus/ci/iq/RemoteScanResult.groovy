/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import com.sonatype.insight.scan.model.Scan
import com.sonatype.nexus.api.iq.scan.ScanResult

import hudson.FilePath

class RemoteScanResult
    implements Serializable
{
  private final Scan scan

  private final FilePath filePath

  RemoteScanResult(Scan scan, FilePath filePath) {
    this.filePath = filePath
    this.scan = scan
  }

  ScanResult copyToLocalScanResult() {
    def localFile = File.createTempFile("scan", ".xml.gz")
    localFile.deleteOnExit()
    new FileOutputStream(localFile).withStream { filePath.copyTo(it) }
    return new ScanResult(scan, localFile)
  }
}
