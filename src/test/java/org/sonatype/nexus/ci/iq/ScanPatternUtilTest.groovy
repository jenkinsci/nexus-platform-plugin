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
