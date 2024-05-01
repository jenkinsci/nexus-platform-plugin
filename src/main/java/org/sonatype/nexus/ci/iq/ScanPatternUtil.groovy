package org.sonatype.nexus.ci.iq

class ScanPatternUtil
{
  static final List<String> DEFAULT_SCAN_PATTERN =
      ['**/*.jar', '**/*.war', '**/*.ear', '**/*.zip', '**/*.tar.gz']

  public static final String EXCLUDE_MARKER = '!'

  // Warning: Only use this in the context of a MasterToSlaveCallable
  static List<File> getScanTargets(final File workDir, final List<String> scanPatterns) {
    def directoryScanner = RemoteScannerFactory.getDirectoryScanner()
    def normalizedScanPatterns = scanPatterns ?: DEFAULT_SCAN_PATTERN
    def includeScanPatterns = normalizedScanPatterns.findAll{!it.startsWith(EXCLUDE_MARKER)}
    def excludeScanPatterns = normalizedScanPatterns.findAll{it.startsWith(EXCLUDE_MARKER)}.collect{it.substring(1)}
    directoryScanner.setBasedir(workDir)
    directoryScanner.setIncludes(includeScanPatterns.toArray(new String[includeScanPatterns.size()]))
    directoryScanner.setExcludes(excludeScanPatterns.toArray(new String[excludeScanPatterns.size()]))
    directoryScanner.addDefaultExcludes()
    directoryScanner.scan()
    return (directoryScanner.getIncludedDirectories() + directoryScanner.getIncludedFiles())
        .collect { f -> new File(workDir, f) }
        .sort()
        .asImmutable()
  }
}
