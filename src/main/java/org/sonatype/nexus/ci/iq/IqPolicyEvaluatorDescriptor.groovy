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

import hudson.model.Job
import hudson.util.FormValidation
import hudson.util.ListBoxModel

interface IqPolicyEvaluatorDescriptor
{
  FormValidation doCheckIqStage(String value)

  ListBoxModel doFillIqStageItems(String jobCredentialsId, Job job)

  FormValidation doCheckListAppId(String value)

  FormValidation doCheckManualAppId(String value)

  ListBoxModel doFillListAppIdItems(String jobCredentialsId, Job job)

  FormValidation doCheckScanPattern(String scanPattern)

  FormValidation doCheckModuleExclude(String moduleExclude)

  FormValidation doCheckFailBuildOnNetworkError(String value)

  ListBoxModel doFillJobCredentialsIdItems(Job job)

  FormValidation doVerifyCredentials(String jobCredentialsId, Job job)
}
