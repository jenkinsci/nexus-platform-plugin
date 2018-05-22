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
package org.sonatype.nexus.ci.nxrm.v3;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

import com.sonatype.nexus.api.exception.RepositoryManagerException;
import com.sonatype.nexus.api.repository.v3.ComponentInfo;
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client;

import org.sonatype.nexus.ci.config.NxrmVersion;
import org.sonatype.nexus.ci.util.FormUtil;
import org.sonatype.nexus.ci.util.NxrmUtil;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import static com.sonatype.nexus.api.common.ArgumentUtils.checkArgument;
import static com.sonatype.nexus.api.common.NexusStringUtils.isNotBlank;
import static hudson.model.Result.FAILURE;
import static java.util.stream.Collectors.joining;
import static org.sonatype.nexus.ci.nxrm.Messages.Common_Validation_NexusInstanceIDRequired;
import static org.sonatype.nexus.ci.nxrm.Messages.Common_Validation_Staging_TagNameRequired;
import static org.sonatype.nexus.ci.nxrm.Messages.DeleteComponents_DisplayName;
import static org.sonatype.nexus.ci.util.RepositoryManagerClientUtil.nexus3Client;

public class DeleteComponentsStep
    extends Builder
    implements SimpleBuildStep
{
  private final String nexusInstanceId;

  private final String tagName;

  @DataBoundConstructor
  public DeleteComponentsStep(final String nexusInstanceId, final String tagName) {
    this.nexusInstanceId = checkArgument(nexusInstanceId, isNotBlank(nexusInstanceId),
        Common_Validation_NexusInstanceIDRequired());
    this.tagName = checkArgument(tagName, isNotBlank(tagName), Common_Validation_Staging_TagNameRequired());
  }

  public String getNexusInstanceId() {
    return nexusInstanceId;
  }

  public String getTagName() {
    return tagName;
  }

  @Override
  public void perform(@Nonnull final Run run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher,
                      @Nonnull final TaskListener listener) throws InterruptedException, IOException
  {
    try {
      RepositoryManagerV3Client client = nexus3Client(nexusInstanceId);
      List<ComponentInfo> components = client.delete(tagName);
      listener.getLogger().println("Delete successful. Components deleted:\n" +
          components.stream()
              .map(c -> c.getGroup() + ":" + c.getName() + ":" + c.getVersion())
              .collect(joining("\n")));
    }
    catch (RepositoryManagerException e) {
      listener.getLogger().println("Failing build due to: " + e.getResponseMessage().orElse(e.getMessage()));
      run.setResult(FAILURE);
    }
  }

  @Extension
  @Symbol("deleteComponents")
  public static final class DescriptorImpl
      extends BuildStepDescriptor<Builder>
  {
    @Override
    public String getDisplayName() {
      return DeleteComponents_DisplayName();
    }

    @Override
    public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
      return true;
    }

    public FormValidation doCheckNexusInstanceId(@QueryParameter final String value) {
      return NxrmUtil.doCheckNexusInstanceId(value);
    }

    public ListBoxModel doFillNexusInstanceIdItems() {
      return NxrmUtil.doFillNexusInstanceIdItems(NxrmVersion.NEXUS_3);
    }

    public FormValidation doCheckTagName(@QueryParameter final String tagName) {
      return FormUtil.validateNotEmpty(tagName, Common_Validation_Staging_TagNameRequired());
    }
  }
}
