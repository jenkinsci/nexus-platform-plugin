/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import hudson.FilePath
import hudson.Launcher
import hudson.model.Run
import hudson.model.TaskListener

trait IqPolicyEvaluator
{
  static final List<String> DEFAULT_SCAN_PATTERN = ["**/*.jar", "**/*.war", "**/*.ear", "**/*.zip", "**/*.tar.gz"]

  String iqStage

  String iqApplication

  List<ScanPattern> iqScanPatterns

  Boolean failBuildOnNetworkError

  String jobCredentialsId

  void evaluatePolicy(final Run run, final FilePath workspace, final Launcher launcher, final TaskListener listener) {
    def client = IqClientFactory.getIqClient()
    def iqPolicyEvaluator = IqApplicationEvaluatorFactory.getPolicyEvaluator(client)
    def targets = iqScanPatterns.collect { it.scanPattern } - null - ""
    if (targets.isEmpty()) {
      targets = DEFAULT_SCAN_PATTERN
    }
    def evaluationResult = iqPolicyEvaluator.performScan(iqApplication, iqStage, targets, workspace)

    // TODO INT-99 will expand this skeleton implementation
    if (evaluationResult.hasWarnings()) {
      listener.getLogger().println("WARNING: IQ Server evaluation of application {} detected warnings.")
    }

    // TODO INT-99 will expand this skeleton implementation
    if (evaluationResult.hasFailures()) {
      PrintWriter errorWriter = listener.fatalError("IQ Server evaluation of application %s failed.", iqApplication)
      errorWriter.println("Meaning error description goes here")
      throw new IqPolicyEvaluationException("Policy violations found")
    }
  }
}
