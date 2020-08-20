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
package org.sonatype.nexus.ci.iq;

import org.sonatype.nexus.ci.util.IqUtil;

import hudson.Extension;
import hudson.RelativePath;
import hudson.model.Job;
import hudson.util.ListBoxModel;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class SelectedApplication
    extends IqApplication
{
  @DataBoundConstructor
  public SelectedApplication(final String applicationId) {
    super(applicationId);
  }

  @Symbol("selectedApplication")
  @Extension
  public static class DescriptorImpl
      extends IqApplicationDescriptor
  {
    public DescriptorImpl() {
      super(SelectedApplication.class);
    }

    @Override
    public String getDisplayName() {
      return Messages.IqPolicyEvaluation_SelectApplication();
    }

    public ListBoxModel doFillApplicationIdItems(@RelativePath("..") @QueryParameter String jobCredentialsId,
                                                 @AncestorInPath Job job,
                                                 @RelativePath("..") @QueryParameter String iqServerId)
    {
      // JobCredentialsId is an empty String if not set
      return IqUtil.doFillIqApplicationItems(jobCredentialsId, job, iqServerId);
    }
  }
}
