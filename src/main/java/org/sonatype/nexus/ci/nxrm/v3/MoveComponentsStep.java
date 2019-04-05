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
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.sonatype.nexus.api.exception.RepositoryManagerException;
import com.sonatype.nexus.api.repository.v3.ComponentInfo;
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client;
import com.sonatype.nexus.api.repository.v3.SearchBuilder;

import org.sonatype.nexus.ci.config.NxrmVersion;
import org.sonatype.nexus.ci.util.FormUtil;
import org.sonatype.nexus.ci.util.Nxrm3Util;
import org.sonatype.nexus.ci.util.NxrmUtil;

import hudson.EnvVars;
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
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import static com.google.common.collect.Maps.newHashMap;
import static com.sonatype.nexus.api.common.ArgumentUtils.checkArgument;
import static com.sonatype.nexus.api.common.NexusStringUtils.isNotBlank;
import static hudson.Util.fixEmptyAndTrim;
import static hudson.model.Result.FAILURE;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.joining;
import static org.sonatype.nexus.ci.nxrm.Messages.Common_Validation_NexusInstanceIDRequired;
import static org.sonatype.nexus.ci.nxrm.Messages.Common_Validation_Staging_TagNameRequired;
import static org.sonatype.nexus.ci.nxrm.Messages.MoveComponents_DisplayName;
import static org.sonatype.nexus.ci.nxrm.Messages.MoveComponents_Validation_DestinationRequired;
import static org.sonatype.nexus.ci.util.RepositoryManagerClientUtil.nexus3Client;

public class MoveComponentsStep
    extends Builder
    implements SimpleBuildStep
{
  private final String nexusInstanceId;

  private final String destination;

  private final Map<String, String> search = newHashMap();

  private String tagName;

  @DataBoundConstructor
  public MoveComponentsStep(final String nexusInstanceId, final String destination)
  {
    this.nexusInstanceId = checkArgument(nexusInstanceId, isNotBlank(nexusInstanceId),
        Common_Validation_NexusInstanceIDRequired());
    this.destination = checkArgument(destination, isNotBlank(destination),
        MoveComponents_Validation_DestinationRequired());
  }

  public String getNexusInstanceId() {
    return nexusInstanceId;
  }

  public String getDestination() {
    return destination;
  }

  public Map<String, String> getSearch() { return unmodifiableMap(search);}

  @DataBoundSetter
  public void setSearch(final Map<String, String> search) {
    this.search.clear();

    if (search != null) {
      this.search.putAll(search);
    }
  }

  @CheckForNull
  public String getTagName() {
    return tagName;
  }

  @DataBoundSetter
  public void setTagName(@CheckForNull final String tagName) {
    this.tagName = fixEmptyAndTrim(tagName);
  }

  @Override
  public void perform(@Nonnull final Run run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher,
                      @Nonnull final TaskListener listener) throws InterruptedException, IOException
  {
    try {
      EnvVars env = run.getEnvironment(listener);
      String resolvedTagName = env.expand(tagName);

      RepositoryManagerV3Client client = nexus3Client(nexusInstanceId);
      SearchBuilder searchBuilder = SearchBuilder.create();
      search.forEach(searchBuilder::withParameter);
      if (isNotBlank(resolvedTagName)) { // explicit tag will take priority if one was also supplied
        searchBuilder.withTag(resolvedTagName);
      }
      List<ComponentInfo> components = client.move(destination, searchBuilder.build());
      listener.getLogger().println("Move successful. Destination: '" + destination + "' Components moved:\n" +
          components.stream().map(ComponentInfo::toString).collect(joining("\n")));
    }
    catch (RepositoryManagerException e) {
      listener.getLogger().println("Failing build due to: " + e.getResponseMessage().orElse(e.getMessage()));
      run.setResult(FAILURE);
      throw new IOException(e);
    }
  }

  @Extension
  @Symbol("moveComponents")
  public static final class DescriptorImpl
      extends BuildStepDescriptor<Builder>
  {
    @Override
    public String getDisplayName() {
      return MoveComponents_DisplayName();
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

    public FormValidation doCheckDestination(@QueryParameter final String value) {
      return NxrmUtil.doCheckNexusRepositoryId(value);
    }

    public ListBoxModel doFillDestinationItems(@QueryParameter final String nexusInstanceId) {
      return Nxrm3Util.doFillNexusHostedRepositoryIdItems(nexusInstanceId);
    }

    public FormValidation doCheckTagName(@QueryParameter final String tagName) {
      return FormUtil.validateNotEmpty(tagName, Common_Validation_Staging_TagNameRequired());
    }
  }
}
