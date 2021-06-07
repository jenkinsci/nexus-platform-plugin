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
import com.sonatype.nexus.api.iq.impl.DefaultIqClient
import com.sonatype.nexus.api.iq.internal.InternalIqClient
import com.sonatype.nexus.api.iq.internal.InternalIqClientBuilder
import com.sonatype.nexus.api.iq.scan.ScanResult

import hudson.FilePath
import hudson.PluginManager
import hudson.PluginWrapper
import jenkins.model.Jenkins
import org.codehaus.plexus.util.DirectoryScanner
import org.slf4j.Logger
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.mop.ConfineMetaClassChanges

@ConfineMetaClassChanges([IqClientFactory])
class RemoteScannerNoMocksTest
    extends Specification
{
  @Shared
  ProprietaryConfig proprietaryConfig = new ProprietaryConfig([], [])

  Logger log

  def setup() {
    GroovyMock(Jenkins, global: true)
    Jenkins jenkins = Mock(Jenkins)
    Jenkins.instance >> jenkins

    PluginManager pluginManager = Mock()
    jenkins.pluginManager >> pluginManager

    PluginWrapper pluginWrapper = Mock()
    pluginManager.getPlugin(_) >> pluginWrapper

    log = Stub()
  }

  def 'scans a pom.xml file'() {
    setup:
      def testPath = getClass().getResource('manifests').path
      def workspaceFile = new File(testPath)
      def remoteScanner = new RemoteScanner('appId', 'stageId', ['*.*'], [], new FilePath(workspaceFile),
          proprietaryConfig, log, 'instance-id', new Properties(), null)

    when:
      def targets = remoteScanner.getScanTargets(workspaceFile, ['*.*'])

    then:
      targets.size() == 1

    when:
      RemoteScanResult scanResult = remoteScanner.call()

    then:
      scanResult
      //1 * iqClient.scan(*_) >> new ScanResult(null, new File('file'))
      //1 * IqClientFactory.getIqLocalClient(log, 'instance-id') >> iqClient
  }
}
