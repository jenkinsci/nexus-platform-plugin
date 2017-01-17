/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import com.sonatype.nexus.api.iq.IqClient

import hudson.FilePath
import org.codehaus.plexus.util.DirectoryScanner
import spock.lang.Specification

class IqApplicationEvaluatorTest
    extends Specification
{
  def static workspace = new FilePath(new File("/tmp/FilePath"))

  IqClient iqClient = Mock(IqClient)

  DirectoryScanner directoryScanner = Mock(DirectoryScanner)

  IqApplicationEvaluator iqApplicationEvaluator = new IqApplicationEvaluator(iqClient, directoryScanner)

  def "creates a list of targets from the result of a directory scan"() {
    setup:
      directoryScanner.getIncludedDirectories() >> matchedDirs.toArray(new String[matchedDirs.size()])
      directoryScanner.getIncludedFiles() >> matchedFiles.toArray(new String[matchedFiles.size()])

    when:
      iqApplicationEvaluator.performScan("appId", "stageId", patterns, workspace)

    then:
      1 * iqClient.evaluateApplication("appId", _, expectedFiles, "stageId")

    where:
      patterns              | matchedFiles          | matchedDirs
      ["aaa", "bbb", "ccc"] | ["ddd", "eee", "fff"] | ["ggg", "hhh", "iii"]

      expectedFiles = (matchedFiles + matchedDirs).collect { new File(workspace.getRemote(), it) }
  }
}
