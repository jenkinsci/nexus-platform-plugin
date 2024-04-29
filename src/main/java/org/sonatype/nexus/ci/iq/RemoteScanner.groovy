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

import hudson.FilePath
import jenkins.security.MasterToSlaveCallable
import org.codehaus.plexus.util.DirectoryScanner
import org.slf4j.Logger

class RemoteScanner
    extends MasterToSlaveCallable<RemoteScanResult, RuntimeException>
{
  static final List<String> DEFAULT_SCAN_PATTERN =
      ['**/*.jar', '**/*.war', '**/*.ear', '**/*.zip', '**/*.tar.gz']

  static final List<String> DEFAULT_MODULE_INCLUDES =
      ['**/sonatype-clm/module.xml', '**/nexus-iq/module.xml']

  public static final String CONTAINER = 'container:'

  public static final String EXCLUDE_MARKER = '!'

  private final String appId

  private final String stageId

  private final List<String> scanPatterns

  private final List<String> moduleExcludes

  private final FilePath workspace

  private final ProprietaryConfig proprietaryConfig

  private final Logger log

  private final String instanceId

  private final Properties advancedProperties

  private final Map<String, String> envVars

  private final Set<String> licensedFeatures

  @SuppressWarnings('ParameterCount')
  RemoteScanner(final String appId,
                final String stageId,
                final List<String> scanPatterns,
                final List<String> moduleExcludes,
                final FilePath workspace,
                final ProprietaryConfig proprietaryConfig,
                final Logger log,
                final String instanceId,
                final Properties advancedProperties,
                final Map<String, String> envVars,
                final Set<String> licensedFeatures = Collections.emptySet())
  {
    this.appId = appId
    this.stageId = stageId
    this.scanPatterns = scanPatterns
    this.moduleExcludes = moduleExcludes
    this.workspace = workspace
    this.proprietaryConfig = proprietaryConfig
    this.log = log
    this.instanceId = instanceId
    this.advancedProperties = advancedProperties
    this.envVars = envVars
    this.licensedFeatures = licensedFeatures
  }

  @Override
  RemoteScanResult call() throws RuntimeException {
    setClassLoaderForCurrentThread()

    InternalIqClient iqClient = IqClientFactory.getIqLocalClient(log, instanceId)
    def workDirectory = new File(workspace.getRemote())
    def targets = getScanTargets(workDirectory, scanPatterns)
    for (String pattern : scanPatterns) {
      if (pattern.startsWith(CONTAINER)) {
        targets = targets.toList()
        targets.add(new File(pattern))
        targets = Collections.unmodifiableList(targets)
      } else {
        if (scanPatterns != null || !scanPatterns.isEmpty()) {
          def filesExcludes = scanPatterns.findAll { it.startsWith('!') }
              .collect { it.substring(1) }.join(',')
          advancedProperties.setProperty("fileExcludes", filesExcludes)
        }
        log.debug("[RemoteScanner-Jenkins] fileExcludes: {}", advancedProperties.getProperty("fileExcludes"))
      }
    }
    def moduleIndices = getModuleIndices(workDirectory, moduleExcludes)
    def scanResult = iqClient.scan(appId, proprietaryConfig, advancedProperties, targets, moduleIndices, workDirectory,
        envVars, licensedFeatures)
    return new RemoteScanResult(scanResult.scan, new FilePath(scanResult.scanFile))
  }

  List<File> getScanTargets(final File workDir, final List<String> scanPatterns) {
    def directoryScanner = RemoteScannerFactory.getDirectoryScanner()
    def normalizedScanPatterns = scanPatterns ?: DEFAULT_SCAN_PATTERN
    def includeScanPatterns = normalizedScanPatterns.findAll{!it.startsWith(EXCLUDE_MARKER)}
    def excludes = [] as List
    for (pattern in normalizedScanPatterns) {
      if (pattern.startsWith('!')) {
        excludes.add(pattern.substring(1))
      }
    }
    //def excludeScanPatterns = normalizedScanPatterns.findAll{it.startsWith(EXCLUDE_MARKER)}.collect{it.substring(1)}
    directoryScanner.setBasedir(workDir)
    directoryScanner.setIncludes(includeScanPatterns.toArray(new String[includeScanPatterns.size()]))
    directoryScanner.setExcludes(excludes.toArray(new String[excludes.size()]))
    directoryScanner.addDefaultExcludes()
    directoryScanner.scan()
       return (directoryScanner.getIncludedDirectories() + directoryScanner.getIncludedFiles())
        .collect { f -> new File(workDir, f) }
        .sort()
        .asImmutable()
  }

  List<File> getModuleIndices(final File workDirectory, final List<String> moduleExcludes) {
    final DirectoryScanner directoryScanner = RemoteScannerFactory.getDirectoryScanner()
    directoryScanner.setBasedir(workDirectory)
    directoryScanner.setIncludes(DEFAULT_MODULE_INCLUDES.toArray(new String[DEFAULT_MODULE_INCLUDES.size()]))
    if (moduleExcludes) {
      directoryScanner.setExcludes(moduleExcludes.toArray(new String[moduleExcludes.size()]))
    }
    directoryScanner.addDefaultExcludes()
    directoryScanner.scan()
    return directoryScanner.getIncludedFiles()
        .collect { f -> new File(workDirectory, f) }
        .sort()
        .asImmutable()
  }

  /**
   * Make sure the plugin class loader is used as a context class loader by the current thread.
   * Jenkins uses a class loader hierarchy and some old libraries assume there is only one class loader (e.g. stax-api).
   * Class loading in Jenkins: https://www.jenkins.io/doc/developer/plugin-development/dependencies-and-class-loading/
   */
  private void setClassLoaderForCurrentThread() {
    // get plugin's ClassLoader
    ClassLoader classLoader = this.class.classLoader
    // set context ClassLoader for current thread
    Thread.currentThread().setContextClassLoader(classLoader)
  }
}
