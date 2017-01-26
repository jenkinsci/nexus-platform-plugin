/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import com.sonatype.nexus.api.iq.ProprietaryConfig
import com.sonatype.nexus.api.iq.internal.InternalIqClient
import com.sonatype.nexus.api.iq.internal.InternalIqClientBuilder
import com.sonatype.nexus.api.iq.scan.ScanResult

import hudson.FilePath
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
    iqClientBuilder.withServerConfig(_) >> iqClientBuilder
    iqClientBuilder.build() >> iqClient
  }

  def "creates a list of targets from the result of a directory scan"() {
    setup:
      def remoteScanner = new RemoteScanner('appId', 'stageId', ['*jar'], workspace, URI.create('http://server/path'), proprietaryConfig, log)
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
}
