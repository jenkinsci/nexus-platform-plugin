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
    def localFile = File.createTempFile('scan', '.xml.gz')
    localFile.deleteOnExit()
    new FileOutputStream(localFile).withStream { filePath.copyTo(it) }
    return new ScanResult(scan, localFile)
  }

  static boolean deleteLocalScanResult(final ScanResult scanResult) {
    scanResult?.scanFile?.delete()
  }
}
