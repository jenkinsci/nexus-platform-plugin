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

import com.sonatype.nexus.api.common.CallflowOptions
import org.sonatype.nexus.ci.config.NxiqConfiguration
import org.sonatype.nexus.ci.util.FormUtil
import org.sonatype.nexus.ci.util.IqUtil

import hudson.Extension
import hudson.Launcher
import hudson.model.AbstractBuild
import hudson.model.AbstractProject
import hudson.model.BuildListener
import hudson.model.Job
import hudson.tasks.BuildStep
import hudson.tasks.BuildStepDescriptor
import hudson.tasks.Builder
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import org.kohsuke.stapler.AncestorInPath
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import org.kohsuke.stapler.QueryParameter

class IqPolicyEvaluatorBuildStep
    extends Builder
    implements IqPolicyEvaluator, BuildStep
{
  String iqInstanceId

  String iqStage

  String iqOrganization

  IqApplication iqApplication

  List<ScanPattern> iqScanPatterns

  List<ModuleExclude> iqModuleExcludes

  Boolean failBuildOnNetworkError

  Boolean failBuildOnScanningErrors

  String jobCredentialsId

  Boolean enableDebugLogging

  String advancedProperties

  CallflowOptions callFlowOptions;

  List<String> callflowScanPatterns;

  @DataBoundConstructor
  @SuppressWarnings('ParameterCount')
  IqPolicyEvaluatorBuildStep(final String iqInstanceId,
                             final String iqStage,
                             final String iqOrganization,
                             final IqApplication iqApplication,
                             final List<ScanPattern> iqScanPatterns,
                             final List<ModuleExclude> iqModuleExcludes,
                             final Boolean failBuildOnNetworkError,
                             final Boolean failBuildOnScanningErrors,
                             final String jobCredentialsId,
                             final Boolean enableDebugLogging,
                             final String advancedProperties,
                             final CallflowOptions callFlowOptions,
                             final String callflowScanPatterns
  )
  {
    this.iqInstanceId = iqInstanceId
    this.jobCredentialsId = jobCredentialsId
    this.failBuildOnNetworkError = failBuildOnNetworkError
    this.failBuildOnScanningErrors = failBuildOnScanningErrors
    this.iqScanPatterns = iqScanPatterns
    this.iqModuleExcludes = iqModuleExcludes
    this.iqStage = iqStage
    this.iqOrganization = iqOrganization
    this.iqApplication = iqApplication
    this.advancedProperties = advancedProperties
    this.enableDebugLogging = enableDebugLogging
    this.callFlowOptions = callFlowOptions
    this.callflowScanPatterns = callflowScanPatterns;
  }

  @DataBoundSetter
  @Override
  void setIqInstanceId(String iqInstanceId) {
    this.iqInstanceId = iqInstanceId ?: IqUtil.getFirstIqConfiguration()?.id
  }

  @Override
  boolean perform(AbstractBuild run, Launcher launcher, BuildListener listener)
      throws InterruptedException, IOException
  {
    IqPolicyEvaluatorUtil.
        evaluatePolicy(this, run, run.getWorkspace(), launcher, listener, run.getEnvironment(listener))
    return true
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
    FormValidation doCheckIqInstanceId(@QueryParameter String value) {
      IqUtil.doCheckIqInstanceId(value)
    }

    @Override
    ListBoxModel doFillIqInstanceIdItems() {
      IqUtil.doFillIqInstanceIdItems()
    }

    @Override
    FormValidation doCheckIqStage(@QueryParameter String value) {
      FormValidation.validateRequired(value)
    }

    @Override
    ListBoxModel doFillIqStageItems(@QueryParameter String iqInstanceId,
                                    @QueryParameter String jobCredentialsId,
                                    @AncestorInPath Job job) {
      NxiqConfiguration nxiqConfiguration = IqUtil.getIqConfiguration(iqInstanceId)
      // JobCredentialsId is an empty String if not set
      IqUtil.doFillIqStageItems(nxiqConfiguration?.serverUrl, jobCredentialsId ?: nxiqConfiguration?.credentialsId, job)
    }

    @Override
    FormValidation doCheckAdvancedProperties(@QueryParameter String advancedProperties) {
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
    FormValidation doCheckFailBuildOnScanningErrors(@QueryParameter String value) {
      FormValidation.validateRequired(value)
    }

    @Override
    FormValidation doCheckEnableDebugLogging(@QueryParameter String value) {
      FormValidation.validateRequired(value)
    }

    @Override
    ListBoxModel doFillJobCredentialsIdItems(@QueryParameter String iqInstanceId, @AncestorInPath Job job)
    {
      NxiqConfiguration nxiqConfiguration = IqUtil.getIqConfiguration(iqInstanceId)
      FormUtil.newCredentialsItemsListBoxModel(nxiqConfiguration?.serverUrl, nxiqConfiguration?.credentialsId, job)
    }

    @Override
    FormValidation doVerifyCredentials(@QueryParameter String iqInstanceId,
                                       @QueryParameter String jobCredentialsId,
                                       @AncestorInPath Job job)
    {
      NxiqConfiguration nxiqConfiguration = IqUtil.getIqConfiguration(iqInstanceId)
      IqUtil.
          verifyJobCredentials(nxiqConfiguration?.serverUrl, jobCredentialsId ?: nxiqConfiguration?.credentialsId, job)
    }

    @Override
    FormValidation doCheckIqOrganization(@QueryParameter String value) {
      FormValidation.ok()
    }

    @Override
    ListBoxModel doFillIqOrganizationItems(@QueryParameter String iqInstanceId,
                                           @QueryParameter String jobCredentialsId,
                                           @AncestorInPath Job job) {
      NxiqConfiguration nxiqConfiguration = IqUtil.getIqConfiguration(iqInstanceId)
      def serverUrl = nxiqConfiguration?.serverUrl
      def jobCredentials = jobCredentialsId ?: nxiqConfiguration?.credentialsId
      IqUtil.doFillIqOrganizationItems(serverUrl, jobCredentials, job)
    }

    CallflowOptions getCallFlowOptions() {
      return this.callFlowOptions;
    }

    String getCallflowScanPatterns() {
      return this.callflowScanPatterns;
    }
  }
}
