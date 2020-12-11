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

import org.sonatype.nexus.ci.config.NxiqConfiguration
import org.sonatype.nexus.ci.util.FormUtil
import org.sonatype.nexus.ci.util.IqUtil

import hudson.Extension
import hudson.model.Job
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import org.jenkinsci.plugins.structs.describable.UninstantiatedDescribable
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl
import org.kohsuke.stapler.AncestorInPath
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import org.kohsuke.stapler.QueryParameter

class IqPolicyEvaluatorWorkflowStep
    extends AbstractStepImpl
    implements IqPolicyEvaluator
{
  String iqStage

  IqApplication iqApplication

  List<ScanPattern> iqScanPatterns

  List<ModuleExclude> iqModuleExcludes

  Boolean failBuildOnNetworkError

  String jobCredentialsId

  Boolean enableDebugLogging

  String terraformPlan

  String advancedProperties

  @DataBoundSetter
  public void setIqScanPatterns(final List<ScanPattern> iqScanPatterns) {
    this.iqScanPatterns = iqScanPatterns
  }

  @DataBoundSetter
  public void setIqModuleExcludes(final List<ModuleExclude> iqModuleExcludes) {
    this.iqModuleExcludes = iqModuleExcludes
  }

  @DataBoundSetter
  public void setFailBuildOnNetworkError(final boolean failBuildOnNetworkError) {
    this.failBuildOnNetworkError = failBuildOnNetworkError
  }

  @DataBoundSetter
  public void setJobCredentialsId(final String jobCredentialsId) {
    this.jobCredentialsId = jobCredentialsId
  }

  @DataBoundSetter
  public void setEnableDebugLogging(final boolean enableDebugLogging) {
    this.enableDebugLogging = enableDebugLogging
  }

  @DataBoundSetter
  public void setTerraformPlan(final String terraformPlan) {
    this.terraformPlan = terraformPlan
  }

  @DataBoundSetter
  void setAdvancedProperties(final String advancedProperties) {
    this.advancedProperties = advancedProperties
  }

  @SuppressWarnings('Instanceof')
  @DataBoundSetter
  public void setIqApplication(final Object iqApplication) {
    if (iqApplication instanceof IqApplication) {
      this.iqApplication = iqApplication
    }
    else if (iqApplication instanceof String) {
      this.iqApplication = new SelectedApplication(iqApplication)
    }
    else if (iqApplication instanceof UninstantiatedDescribable) {
      this.iqApplication = ((UninstantiatedDescribable) iqApplication).instantiate()
    }
    else {
      throw new IllegalArgumentException("iqApplication is not a valid format")
    }
  }

  @DataBoundConstructor
  IqPolicyEvaluatorWorkflowStep(final String iqStage)
  {
    this.iqStage = iqStage
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
    ListBoxModel doFillIqStageItems(@QueryParameter String jobCredentialsId, @AncestorInPath Job job) {
      IqUtil.doFillIqStageItems(jobCredentialsId, job)
    }

    @Override
    FormValidation doCheckAdvancedProperties(@QueryParameter String advancedProperties) {
      FormValidation.ok()
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
    FormValidation doCheckFailBuildOnNetworkError(@QueryParameter final String value) {
      FormValidation.validateRequired(value)
    }

    @Override
    ListBoxModel doFillJobCredentialsIdItems(@AncestorInPath final Job job) {
      FormUtil.newCredentialsItemsListBoxModel(NxiqConfiguration.serverUrl.toString(), NxiqConfiguration.credentialsId,
          job)
    }

    @Override
    FormValidation doCheckEnableDebugLogging(@QueryParameter final String value) {
      FormValidation.validateRequired(value)
    }

    @Override
    FormValidation doCheckTerraformPlan(@QueryParameter final String value) {
      FormValidation.ok()
    }

    @Override
    FormValidation doVerifyCredentials(@QueryParameter String jobCredentialsId, @AncestorInPath Job job)
    {
      IqUtil.verifyJobCredentials(jobCredentialsId, job)
    }
  }
}
