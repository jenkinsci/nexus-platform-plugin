/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import javax.annotation.Nonnull

import com.sonatype.nexus.ci.config.NxiqConfiguration
import com.sonatype.nexus.ci.util.FormUtil
import com.sonatype.nexus.ci.util.IqUtil

import hudson.Extension
import hudson.FilePath
import hudson.Launcher
import hudson.model.AbstractProject
import hudson.model.Run
import hudson.model.TaskListener
import hudson.tasks.BuildStepDescriptor
import hudson.tasks.Builder
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import jenkins.tasks.SimpleBuildStep
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

class IqPolicyEvaluatorBuildStep
    extends Builder
    implements IqPolicyEvaluator, SimpleBuildStep
{
  static final List<String> DEFAULT_SCAN_PATTERN = ["**/*.jar", "**/*.war", "**/*.ear", "**/*.zip", "**/*.tar.gz"]

  String iqStage

  String iqApplication

  List<ScanPattern> iqScanPatterns

  Boolean failBuildOnNetworkError

  String jobCredentialsId

  @DataBoundConstructor
  IqPolicyEvaluatorBuildStep(final String iqStage, final String iqApplication, final List<ScanPattern> iqScanPatterns,
                             final Boolean failBuildOnNetworkError, final String jobCredentialsId)
  {
    this.jobCredentialsId = jobCredentialsId
    this.failBuildOnNetworkError = failBuildOnNetworkError
    this.iqScanPatterns = iqScanPatterns
    this.iqApplication = iqApplication
    this.iqStage = iqStage
  }

  @Override
  void perform(@Nonnull final Run run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher,
               @Nonnull final TaskListener listener) throws InterruptedException, IOException
  {
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

  @Extension
  static final class PolicyEvaluatorDescriptorImpl
      extends BuildStepDescriptor<Builder>
      implements IqPolicyEvaluatorDescriptor
  {
    @Override
    String getDisplayName() {
      'Nexus IQ Policy Evaluator'
    }

    @Override
    boolean isApplicable(final Class<? extends AbstractProject> jobType) {
      return true
    }

    @Override
    FormValidation doCheckIqStage(@QueryParameter String value) {
      FormValidation.validateRequired(value)
    }

    @Override
    ListBoxModel doFillIqStageItems() {
      IqUtil.doFillIqStageItems()
    }

    @Override
    FormValidation doCheckIqApplication(@QueryParameter String value) {
      FormValidation.validateRequired(value)
    }

    @Override
    ListBoxModel doFillIqApplicationItems() {
      IqUtil.doFillIqApplicationItems()
    }

    @Override
    FormValidation doCheckScanPattern(@QueryParameter String value) {
      FormValidation.ok()
    }

    @Override
    FormValidation doCheckFailBuildOnNetworkError(@QueryParameter String value) {
      FormValidation.validateRequired(value)
    }

    @Override
    ListBoxModel doFillJobCredentialsIdItems() {
      return FormUtil.buildCredentialsItems(NxiqConfiguration.serverUrl)
    }
  }
}
