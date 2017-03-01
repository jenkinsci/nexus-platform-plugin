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
import com.sonatype.nexus.api.iq.internal.InternalIqClientBuilder
import com.sonatype.nexus.api.iq.scan.ScanResult

import hudson.FilePath
import jenkins.model.Jenkins
import org.codehaus.plexus.util.DirectoryScanner
import org.slf4j.Logger
import spock.lang.Specification

class RemoteScannerTest
    extends Specification
{
  Logger log
  ProprietaryConfig proprietaryConfig
  InternalIqClient iqClient
  DirectoryScanner directoryScanner

  def setup() {
    GroovyMock(Jenkins, global: true)
    Jenkins.instance >> Mock(Jenkins)

    log = Stub()
    proprietaryConfig = new ProprietaryConfig([], [])
    GroovyMock(InternalIqClientBuilder, global: true)
    GroovyMock(RemoteScannerFactory, global: true)
    directoryScanner = Mock()
    RemoteScannerFactory.getDirectoryScanner() >> directoryScanner
    InternalIqClientBuilder iqClientBuilder = Mock()
    iqClient = Mock()
    InternalIqClientBuilder.create() >> iqClientBuilder
    iqClientBuilder.withLogger(log) >> iqClientBuilder
    iqClientBuilder.withInstanceId(_) >> iqClientBuilder
    iqClientBuilder.build() >> iqClient
  }

  def "creates a list of targets from the result of a directory scan"() {
    setup:
      def remoteScanner = new RemoteScanner('appId', 'stageId', ['*jar'], workspace, proprietaryConfig, log,
          "instance-id")
      directoryScanner.getIncludedDirectories() >> matchedDirs.toArray(new String[matchedDirs.size()])
      directoryScanner.getIncludedFiles() >> matchedFiles.toArray(new String[matchedFiles.size()])

    when:
      remoteScanner.call()

    then:
      iqClient.scan('appId', proprietaryConfig, _, expectedFiles) >> new ScanResult(null, new File('file'))

    where:
      matchedFiles          | matchedDirs
      ["ddd", "eee", "fff"] | ["ggg", "hhh", "iii"]

      workspace = new FilePath(new File('/file/path'))
      expectedFiles = (matchedFiles + matchedDirs).collect { new File(workspace.getRemote(), it) }
  }

  def 'RemoteScanner passes arguments to IqClient'() {
    setup:
      GroovyMock(IqClientFactory, global: true)
      def remoteScanner = new RemoteScanner('appId', 'stageId', ['*jar'], new FilePath(new File('/file/path')),
          proprietaryConfig, log, 'instance-id')
      directoryScanner.getIncludedDirectories() >> []
      directoryScanner.getIncludedFiles() >> []

    when:
      remoteScanner.call()

    then:
      1 * iqClient.scan('appId', proprietaryConfig, _, []) >> new ScanResult(null, new File('file'))
      1 * IqClientFactory.getIqLocalClient(log, 'instance-id') >> iqClient
  }
}
