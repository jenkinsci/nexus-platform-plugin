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
import com.sonatype.nexus.api.repository.v3.SearchBuilder;

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
import static hudson.util.FormValidation.error;
import static hudson.util.FormValidation.ok;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.sonatype.nexus.ci.config.NxrmVersion.NEXUS_3;
import static org.sonatype.nexus.ci.nxrm.Messages.AssociateTag_DisplayName;
import static org.sonatype.nexus.ci.nxrm.Messages.Common_Validation_NexusInstanceIDRequired;
import static org.sonatype.nexus.ci.nxrm.Messages.Common_Validation_Staging_SearchRequired;
import static org.sonatype.nexus.ci.nxrm.Messages.Common_Validation_Staging_TagNameRequired;
import static org.sonatype.nexus.ci.util.FormUtil.validateNotEmpty;
import static org.sonatype.nexus.ci.util.RepositoryManagerClientUtil.nexus3Client;

public class AssociateTagStep
    extends Builder
    implements SimpleBuildStep
{
  private final String nexusInstanceId;

  private final String tagName;

  private final List<SearchCriteria> searchCriteria;

  @DataBoundConstructor
  public AssociateTagStep(final String nexusInstanceId, final String tagName, final List<SearchCriteria> searchCriteria)
  {
    this.nexusInstanceId = checkArgument(nexusInstanceId, isNotBlank(nexusInstanceId),
        Common_Validation_NexusInstanceIDRequired());
    this.tagName = checkArgument(tagName, isNotBlank(tagName), Common_Validation_Staging_TagNameRequired());
    this.searchCriteria = checkArgument(searchCriteria, isNotEmpty(searchCriteria),
        Common_Validation_Staging_SearchRequired());
  }

  public String getNexusInstanceId() {
    return nexusInstanceId;
  }

  public String getTagName() {
    return tagName;
  }

  public List<SearchCriteria> getSearchCriteria() {
    return searchCriteria;
  }

  @Override
  public void perform(@Nonnull final Run run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher,
                      @Nonnull final TaskListener listener) throws InterruptedException, IOException
  {
    RepositoryManagerV3Client client = null;

    try {
      client = nexus3Client(nexusInstanceId);
    }
    catch (RepositoryManagerException e) {
      failBuildAndThrow(run, listener, e.getResponseMessage().orElse(e.getMessage()), new IOException(e));
    }

    try {
      SearchBuilder search = SearchBuilder.create();
      searchCriteria.forEach(sc -> search.withParameter(sc.getKey(), sc.getValue()));
      List<ComponentInfo> associated = client.associate(tagName, search.build());
      listener.getLogger().println("Associate successful for components:\n" +
          associated.stream().map(c -> c.getGroup() + ":" + c.getName() + ":" + c.getVersion()).collect(joining("\n")));
    }
    catch (RepositoryManagerException e) {
      failBuildAndThrow(run, listener, e.getResponseMessage().orElse(e.getMessage()), new IOException(e));
    }
  }

  private void failBuild(Run run, TaskListener listener, String reason) {
    listener.getLogger().println("Failing build due to: " + reason);
    run.setResult(FAILURE);
  }

  private void failBuildAndThrow(Run run, TaskListener listener, String reason, IOException ioException)
      throws IOException
  {
    failBuild(run, listener, reason);
    throw ioException;
  }

  @Extension
  @Symbol("associateTag")
  public static final class DescriptorImpl
      extends BuildStepDescriptor<Builder>
  {
    @Override
    public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return AssociateTag_DisplayName();
    }

    public FormValidation doCheckNexusInstanceId(@QueryParameter String value) {
      return NxrmUtil.doCheckNexusInstanceId(value);
    }

    public ListBoxModel doFillNexusInstanceIdItems() {
      return NxrmUtil.doFillNexusInstanceIdItems(NEXUS_3);
    }

    public FormValidation doCheckTagName(@QueryParameter String tagName) {
      return validateNotEmpty(tagName, Common_Validation_Staging_TagNameRequired());
    }
  }
}
