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

import hudson.FilePath
import spock.lang.Specification

class RemoteScanResultTest extends Specification
{
  def 'create and delete remote scan results'() {
    given: 'a file on an agent'
      final agentFile = File.createTempFile('scan-', '.txt')
      agentFile.withWriter { w -> w.print('content') }
      final filePath = new FilePath(agentFile)
    and: 'a scan'
      final Scan scan = Mock()
    and: 'a new remote scan result wrapped around the scan and agent file'
      final remoteScanResult = new RemoteScanResult(scan, filePath)
    expect: 'we can make a local copy of the result'
      final scanResult = remoteScanResult.copyToLocalScanResult()
      scanResult.scan == scan
      scanResult.scanFile.text == 'content'
    when: 'we delete the remote scan result'
      remoteScanResult.delete()
    then: 'the file is gone from the agent'
      ! agentFile.exists()
  }
}
