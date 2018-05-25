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
package org.sonatype.nexus.ci.nxrm

import org.sonatype.nexus.ci.util.NxrmUtil

import hudson.Extension
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import org.kohsuke.stapler.QueryParameter
import org.kohsuke.stapler.bind.JavaScriptMethod

import static org.sonatype.nexus.ci.config.NxrmVersion.NEXUS_3

class NexusPublisherWorkflowStep
    extends AbstractStepImpl
    implements NexusPublisher
{
  String nexusInstanceId

  String nexusRepositoryId

  List<Package> packages

  private String tagName

  @DataBoundConstructor
  NexusPublisherWorkflowStep(
      final String nexusInstanceId,
      final String nexusRepositoryId,
      final List<Package> packages)
  {
    this.nexusInstanceId = nexusInstanceId
    this.nexusRepositoryId = nexusRepositoryId
    this.packages = packages ?: []
  }

  String getTagName() {
    tagName
  }

  @DataBoundSetter
  void setTagName(final String tagName) {
    this.tagName = tagName?.trim() ? tagName : null
  }

  @Extension
  static class DescriptorImpl
      extends AbstractStepDescriptorImpl
      implements NexusPublisherDescriptor
  {
    DescriptorImpl() {
      super(PackagePublisherExecution)
    }

    @Override
    String getFunctionName() {
      Messages.NexusPublisherWorkflowStep_FunctionName()
    }

    @Override
    String getDisplayName() {
      Messages.NexusPublisherWorkflowStep_DisplayName()
    }

    @Override
    FormValidation doCheckNexusInstanceId(@QueryParameter String value) {
      NxrmUtil.doCheckNexusInstanceId(value)
    }

    @Override
    ListBoxModel doFillNexusInstanceIdItems() {
      NxrmUtil.doFillNexusInstanceIdItems()
    }

    @Override
    FormValidation doCheckNexusRepositoryId(@QueryParameter String value) {
      NxrmUtil.doCheckNexusRepositoryId(value)
    }

    @Override
    ListBoxModel doFillNexusRepositoryIdItems(@QueryParameter String nexusInstanceId) {
      NxrmUtil.doFillNexusRepositoryIdItems(nexusInstanceId)
    }

    @JavaScriptMethod
    static String getTagVisibility(final String id) {
      NxrmUtil.isVersion(id, NEXUS_3) ? 'visible' : 'hidden'
    }
  }
}
