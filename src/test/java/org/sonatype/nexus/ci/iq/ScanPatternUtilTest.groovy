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

import org.codehaus.plexus.util.DirectoryScanner
import spock.lang.Specification
import spock.util.mop.ConfineMetaClassChanges

@ConfineMetaClassChanges([IqClientFactory])
class ScanPatternUtilTest
  extends Specification
{
  DirectoryScanner directoryScanner

  def setup() {
    GroovyMock(RemoteScannerFactory, global: true)
    directoryScanner = Mock()
    RemoteScannerFactory.getDirectoryScanner() >> directoryScanner
  }

  def 'Uses default scan patterns when patterns not set'() {
    setup:
      def workspaceFile = new File('/file/path')
      directoryScanner.getIncludedDirectories() >> []
      directoryScanner.getIncludedFiles() >> []

    when:
      ScanPatternUtil.getScanTargets(workspaceFile, [])

    then:
      1 * directoryScanner.setIncludes(*_) >> { arguments ->
        assert arguments[0] == ScanPatternUtil.DEFAULT_SCAN_PATTERN
      }
  }

  def 'Uses no excludes by default'() {
    setup:
      def workspaceFile = new File('/file/path')
      directoryScanner.getIncludedDirectories() >> []
      directoryScanner.getIncludedFiles() >> []

    when:
      ScanPatternUtil.getScanTargets(workspaceFile, ['*.jar'])

    then:
      1 * directoryScanner.setIncludes(*_) >> { arguments ->
        assert arguments[0] == ['*.jar']
      }
      1 * directoryScanner.setExcludes(*_) >> { arguments ->
        assert arguments[0] == []
      }
  }

  def 'Pass excludes to directory scanner'() {
    setup:
      def workspaceFile = new File('/file/path')
      directoryScanner.getIncludedDirectories() >> []
      directoryScanner.getIncludedFiles() >> []

    when:
      ScanPatternUtil.getScanTargets(workspaceFile, ['*.jar','!*.zip','*.war','!*.tar'])

    then:
      1 * directoryScanner.setIncludes(*_) >> { arguments ->
        assert arguments[0] == ['*.jar','*.war']
      }
      1 * directoryScanner.setExcludes(*_) >> { arguments ->
        assert arguments[0] == ['*.zip','*.tar']
      }
  }
}
