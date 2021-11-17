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
  String iqInstanceId

  String iqStage

  IqApplication iqApplication

  List<ScanPattern> iqScanPatterns

  List<ModuleExclude> iqModuleExcludes

  Boolean failBuildOnNetworkError

  String jobCredentialsId

  Boolean enableDebugLogging

  String advancedProperties

  @DataBoundSetter
  void setIqScanPatterns(final List<ScanPattern> iqScanPatterns) {
    this.iqScanPatterns = iqScanPatterns
  }

  @DataBoundSetter
  void setIqModuleExcludes(final List<ModuleExclude> iqModuleExcludes) {
    this.iqModuleExcludes = iqModuleExcludes
  }

  @DataBoundSetter
  void setFailBuildOnNetworkError(final boolean failBuildOnNetworkError) {
    this.failBuildOnNetworkError = failBuildOnNetworkError
  }

  @DataBoundSetter
  void setJobCredentialsId(final String jobCredentialsId) {
    this.jobCredentialsId = jobCredentialsId
  }

  @DataBoundSetter
  void setEnableDebugLogging(final boolean enableDebugLogging) {
    this.enableDebugLogging = enableDebugLogging
  }

  @DataBoundSetter
  void setAdvancedProperties(final String advancedProperties) {
    this.advancedProperties = advancedProperties
  }

  @SuppressWarnings('Instanceof')
  @DataBoundSetter
  void setIqApplication(final Object iqApplication) {
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
      throw new IllegalArgumentException('iqApplication is not a valid format')
    }
  }

  @DataBoundConstructor
  IqPolicyEvaluatorWorkflowStep(final String iqInstanceId, final String iqStage)
  {
    this.iqInstanceId = iqInstanceId
    this.iqStage = iqStage
  }

  @DataBoundSetter
  @Override
  void setIqInstanceId(String iqInstanceId) {
    this.iqInstanceId = iqInstanceId ?: IqUtil.getFirstIqConfiguration()?.id
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
    FormValidation doCheckIqInstanceId(@QueryParameter final String value) {
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
      IqUtil.doFillIqStageItems(nxiqConfiguration?.serverUrl, jobCredentialsId ?: nxiqConfiguration?.credentialsId, job)
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
    ListBoxModel doFillJobCredentialsIdItems(@QueryParameter String iqInstanceId, @AncestorInPath final Job job) {
      NxiqConfiguration nxiqConfiguration = IqUtil.getIqConfiguration(iqInstanceId)
      FormUtil.newCredentialsItemsListBoxModel(nxiqConfiguration?.serverUrl, nxiqConfiguration?.credentialsId, job)
    }

    @Override
    FormValidation doCheckEnableDebugLogging(@QueryParameter final String value) {
      FormValidation.validateRequired(value)
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
  }
}
