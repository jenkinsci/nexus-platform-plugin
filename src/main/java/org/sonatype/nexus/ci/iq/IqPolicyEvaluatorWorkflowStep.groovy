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

import javax.annotation.Nullable
import javax.annotation.ParametersAreNonnullByDefault

import org.sonatype.nexus.ci.config.NxiqConfiguration
import org.sonatype.nexus.ci.util.FormUtil
import org.sonatype.nexus.ci.util.IqUtil

import hudson.Extension
import hudson.model.Job
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl
import org.kohsuke.stapler.AncestorInPath
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import org.kohsuke.stapler.QueryParameter

@ParametersAreNonnullByDefault
class IqPolicyEvaluatorWorkflowStep
    extends AbstractStepImpl
    implements IqPolicyEvaluator
{
  String iqStage

  String applicationSelectTypePost = 'select'

  String listAppId = ''

  String manualAppId = ''

  List<ScanPattern> iqScanPatterns

  List<ModuleExclude> moduleExcludes

  Boolean failBuildOnNetworkError

  String jobCredentialsId

  @DataBoundSetter
  public void setIqScanPatterns(final List<ScanPattern> iqScanPatterns) {
    this.iqScanPatterns = iqScanPatterns
  }

  @DataBoundSetter
  public void setModuleExcludes(final List<ModuleExclude> moduleExcludes) {
    this.moduleExcludes = moduleExcludes
  }

  @DataBoundSetter
  public void setFailBuildOnNetworkError(final Boolean failBuildOnNetworkError) {
    this.failBuildOnNetworkError = failBuildOnNetworkError
  }

  @DataBoundSetter
  public void setJobCredentialsId(final String jobCredentialsId) {
    this.jobCredentialsId = jobCredentialsId
  }

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
  IqPolicyEvaluatorWorkflowStep(final String iqStage,
                                final String applicationSelectTypePost,
                                final String listAppId,
                                final String manualAppId
  )
  {
    this.iqStage = iqStage
    this.applicationSelectTypePost = applicationSelectTypePost
    if (applicationSelectTypePost == 'select') {
      this.listAppId = listAppId
      this.manualAppId = ''
    }
    else {
      this.listAppId = ''
      this.manualAppId = manualAppId
    }
  }

  @Extension
  static final class DescriptorImpl
      extends AbstractStepDescriptorImpl
      implements IqPolicyEvaluatorDescriptor
  {
    DescriptorImpl() {
      super(PolicyEvaluatorExecution)
    }

    @Override
    String getFunctionName() {
      Messages.IqPolicyEvaluation_FunctionName()
    }

    @Override
    String getDisplayName() {
      Messages.IqPolicyEvaluation_DisplayName()
    }

    @Override
    FormValidation doCheckIqStage(@QueryParameter String value) {
      FormValidation.validateRequired(value)
    }

    @Override
    ListBoxModel doFillIqStageItems(@QueryParameter @Nullable String jobCredentialsId, @AncestorInPath Job job) {
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
    ListBoxModel doFillListAppIdItems(@QueryParameter @Nullable String jobCredentialsId, @AncestorInPath Job job) {
      IqUtil.doFillIqApplicationItems(jobCredentialsId, job)
    }

    @Override
    FormValidation doCheckScanPattern(@QueryParameter final String scanPattern) {
      FormValidation.ok()
    }

    @Override
    FormValidation doCheckModuleExclude(@QueryParameter final String value) {
      FormValidation.ok()
    }

    @Override
    FormValidation doCheckApplicationSelectTypePost(@QueryParameter String applicationSelectTypePost) {
      FormValidation.ok()
    }

    @Override
    FormValidation doCheckFailBuildOnNetworkError(@QueryParameter final String value) {
      FormValidation.validateRequired(value)
    }

    @Override
    ListBoxModel doFillJobCredentialsIdItems(@AncestorInPath final Job job) {
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
