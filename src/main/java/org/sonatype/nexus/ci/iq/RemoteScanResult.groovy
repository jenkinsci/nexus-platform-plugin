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

import java.util.stream.Collector
import java.util.stream.Collectors

import com.sonatype.nexus.api.iq.scan.ScanResult

import com.sonatype.insight.scan.model.Scan
import hudson.FilePath

class RemoteScanResult
    implements Serializable
{
  private final Scan scan

  private final FilePath filePath

  private final List<FilePath> filePaths

  RemoteScanResult(Scan scan, FilePath filePath) {
    this.filePath = filePath
    this.scan = scan
    this.filePaths = [];
  }

  RemoteScanResult(Scan scan, FilePath filePath, List<FilePath> filePaths) {
    this.filePath = filePath
    this.scan = scan
    this.filePaths = filePaths
  }

  /**
   * create a copy as a ScanResult with a local copy of the scan file
   * @return a local ScanResult
   */
  ScanResult copyToLocalScanResult() {
    def localFile = File.createTempFile('scan', '.xml.gz')
    def additionalFiles = filePaths.stream().map {
      File.createTempFile( it.baseName.split("\\.")[0],'.json.gz')
    }.collect(Collectors.toList())
    new FileOutputStream(localFile).withStream {
      filePath.copyTo(it)
    }
    additionalFiles.stream().forEach {
      new FileOutputStream(it).withStream { filePath.copyTo(it) }
    }
    return new ScanResult(scan, localFile, additionalFiles)
  }

  /**
   * delete the remote scan file from the remote agent
   * @return true if the file as deleted
   */
  boolean delete() {
    filePath.delete()
    filePaths.stream().map {path -> path.delete()}.every()
  }
}
