package org.sonatype.nexus.ci.iq

import jenkins.security.MasterToSlaveCallable

class RemoteFileResolver
    extends MasterToSlaveCallable<List<String>, RuntimeException>
{
  private final File workDir;
  private final List<String> scanPatterns;

  RemoteFileResolver(final File workDir,final List<String> scanPatterns) {
    this.workDir = workDir
    this.scanPatterns = scanPatterns
  }

  @Override
  List<String> call() throws RuntimeException {
    return ScanPatternUtil.getScanTargets(workDir, scanPatterns)
        .collect {
          return it.getAbsolutePath()
        }
  }
}
