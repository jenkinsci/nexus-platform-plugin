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

import org.sonatype.nexus.ci.util.IqUtil

import hudson.Extension
import hudson.model.Job
import hudson.util.ListBoxModel
import org.jenkinsci.Symbol
import org.kohsuke.stapler.AncestorInPath
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

class SelectedApplication
    extends IqApplication
{
  @DataBoundConstructor
  SelectedApplication(final String applicationId) {
    super(applicationId)
  }

  @Symbol('selectedApplication')
  @Extension
  static class DescriptorImpl
      extends IqApplication.IqApplicationDescriptor
  {
    DescriptorImpl() {
      super(SelectedApplication)
    }

    @Override
    String getDisplayName() {
      return Messages.IqPolicyEvaluation_SelectApplication()
    }

    ListBoxModel doFillApplicationIdItems(@QueryParameter String jobCredentialsId, @AncestorInPath Job job) {
      // JobCredentialsId is an empty String if not set
      return IqUtil.doFillIqApplicationItems(jobCredentialsId, job)
    }
  }
}
