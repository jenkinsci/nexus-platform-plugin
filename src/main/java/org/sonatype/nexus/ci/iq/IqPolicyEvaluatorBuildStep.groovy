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

import javax.annotation.Nonnull
import javax.annotation.Nullable
import javax.annotation.ParametersAreNonnullByDefault

import org.sonatype.nexus.ci.config.NxiqConfiguration
import org.sonatype.nexus.ci.util.FormUtil
import org.sonatype.nexus.ci.util.IqUtil

import hudson.Extension
import hudson.FilePath
import hudson.Launcher
import hudson.model.AbstractProject
import hudson.model.Job
import hudson.model.Run
import hudson.model.TaskListener
import hudson.tasks.BuildStepDescriptor
import hudson.tasks.Builder
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import jenkins.tasks.SimpleBuildStep
import org.kohsuke.stapler.AncestorInPath
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

@ParametersAreNonnullByDefault
class IqPolicyEvaluatorBuildStep
    extends Builder
    implements IqPolicyEvaluator, SimpleBuildStep
{
  String iqStage

  String applicationSelectTypePost = 'select'

  String listAppId = ''

  String manualAppId = ''

  List<ScanPattern> iqScanPatterns

  List<ModuleExclude> moduleExcludes

  Boolean failBuildOnNetworkError

  String jobCredentialsId

  @Override
  String getApplicationId(){
    if (value == 'select') {
      return manualAppId
    }
    else {
      return listAppId
    }
  }

  @DataBoundConstructor
  @SuppressWarnings('ParameterCount')
  IqPolicyEvaluatorBuildStep(final String iqStage,
                             final String applicationSelectTypePost,
                             final String listAppId,
                             final String manualAppId,
                             final List<ScanPattern> iqScanPatterns,
                             final List<ModuleExclude> moduleExcludes,
                             final Boolean failBuildOnNetworkError,
                             final String jobCredentialsId)
  {
    this.jobCredentialsId = jobCredentialsId
    this.failBuildOnNetworkError = failBuildOnNetworkError
    this.iqScanPatterns = iqScanPatterns
    this.moduleExcludes = moduleExcludes
    this.applicationSelectTypePost = applicationSelectTypePost
    this.iqStage = iqStage
    if (applicationSelectTypePost == 'select') {
      this.listAppId = listAppId
      this.manualAppId = ''
    }
    else {
      this.listAppId = ''
      this.manualAppId = manualAppId
    }
  }

  @Override
  void perform(@Nonnull final Run run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher,
               @Nonnull final TaskListener listener) throws InterruptedException, IOException
  {
    IqPolicyEvaluatorUtil.evaluatePolicy(this, run, workspace, launcher, listener)
  }

  @Extension
  static final class DescriptorImpl
      extends BuildStepDescriptor<Builder>
      implements IqPolicyEvaluatorDescriptor
  {
    @Override
    String getDisplayName() {
      Messages.IqPolicyEvaluation_DisplayName()
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
    ListBoxModel doFillIqStageItems(@QueryParameter String jobCredentialsId, @AncestorInPath Job job) {
      // JobCredentialsId is an empty String if not set
      IqUtil.doFillIqStageItems(jobCredentialsId, job)
    }

    @Override
    FormValidation doCheckListAppId(@QueryParameter String value) {
      FormValidation.validateRequired(value)
    }

    @Override
    FormValidation doCheckManualAppId(@QueryParameter String value) {
      FormValidation.validateRequired(value)
    }

    @Override
    ListBoxModel doFillListAppIdItems(@QueryParameter String jobCredentialsId, @AncestorInPath Job job) {
      // JobCredentialsId is an empty String if not set
      IqUtil.doFillIqApplicationItems(jobCredentialsId, job)
    }

    @Override
    FormValidation doCheckApplicationSelectTypePost(@QueryParameter String applicationSelectTypePost) {
      FormValidation.ok()
    }

    @Override
    FormValidation doCheckScanPattern(@QueryParameter String value) {
      FormValidation.ok()
    }

    @Override
    FormValidation doCheckModuleExclude(@QueryParameter final String value) {
      FormValidation.ok()
    }

    @Override
    FormValidation doCheckFailBuildOnNetworkError(@QueryParameter String value) {
      FormValidation.validateRequired(value)
    }

    @Override
    ListBoxModel doFillJobCredentialsIdItems(@AncestorInPath Job job) {
      FormUtil.newCredentialsItemsListBoxModel(NxiqConfiguration.serverUrl.toString(), NxiqConfiguration.credentialsId,
        job)
    }

    @Override
    FormValidation doVerifyCredentials(@QueryParameter @Nullable String jobCredentialsId, @AncestorInPath Job job)
    {
      IqUtil.verifyJobCredentials(jobCredentialsId, job)
    }
  }
}
