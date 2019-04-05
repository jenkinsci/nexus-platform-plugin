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
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.sonatype.nexus.api.exception.RepositoryManagerException;
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client;
import com.sonatype.nexus.api.repository.v3.Tag;

import org.sonatype.nexus.ci.util.FormUtil;
import org.sonatype.nexus.ci.util.NxrmUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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

import static com.sonatype.nexus.api.common.ArgumentUtils.checkArgument;
import static com.sonatype.nexus.api.common.NexusStringUtils.isBlank;
import static com.sonatype.nexus.api.common.NexusStringUtils.isNotBlank;
import static hudson.Util.fixEmptyAndTrim;
import static hudson.model.Result.FAILURE;
import static hudson.util.FormValidation.error;
import static hudson.util.FormValidation.ok;
import static org.sonatype.nexus.ci.config.NxrmVersion.NEXUS_3;
import static org.sonatype.nexus.ci.nxrm.Messages.Common_Validation_NexusInstanceIDRequired;
import static org.sonatype.nexus.ci.nxrm.Messages.Common_Validation_Staging_TagNameRequired;
import static org.sonatype.nexus.ci.nxrm.Messages.CreateTag_DisplayName;
import static org.sonatype.nexus.ci.nxrm.Messages.CreateTag_Error_TagAttributesJson;
import static org.sonatype.nexus.ci.nxrm.Messages.CreateTag_Error_TagAttributesPath;
import static org.sonatype.nexus.ci.nxrm.Messages.CreateTag_Validation_TagAttributesJson;
import static org.sonatype.nexus.ci.util.RepositoryManagerClientUtil.nexus3Client;

public class CreateTagStep
    extends Builder
    implements SimpleBuildStep
{
  static final Gson GSON = new Gson();

  static final Type ATTRIBUTE_TYPE = new TypeToken<Map<String, Object>>() { }.getType();

  private final String nexusInstanceId;

  private final String tagName;

  private String tagAttributesPath;

  private String tagAttributesJson;

  @DataBoundConstructor
  public CreateTagStep(final String nexusInstanceId, final String tagName)
  {
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

  @CheckForNull
  public String getTagAttributesPath() {
    return tagAttributesPath;
  }

  @DataBoundSetter
  public void setTagAttributesPath(@CheckForNull final String tagAttributesPath) {
    this.tagAttributesPath = fixEmptyAndTrim(tagAttributesPath);
  }

  @CheckForNull
  public String getTagAttributesJson() {
    return tagAttributesJson;
  }

  @DataBoundSetter
  public void setTagAttributesJson(@CheckForNull final String tagAttributesJson) {
    this.tagAttributesJson = fixEmptyAndTrim(tagAttributesJson);
  }

  @Override
  public void perform(@Nonnull final Run run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher,
                      @Nonnull final TaskListener listener) throws InterruptedException, IOException
  {
    RepositoryManagerV3Client client = null;
    Map<String, Object> tagAttributes = (isNotBlank(tagAttributesPath) ||
        isNotBlank(tagAttributesJson)) ? new HashMap<>() : null;
    EnvVars env = run.getEnvironment(listener);

    try {
      client = nexus3Client(nexusInstanceId);
    }
    catch (RepositoryManagerException e) {
      failBuildAndThrow(run, listener, e.getResponseMessage().orElse(e.getMessage()), new IOException(e));
    }

    if (isNotBlank(tagAttributesPath)) {
      try {
        tagAttributes.putAll(parseAttributes(workspace, tagAttributesPath, env));
      }
      catch (Exception e) {
        failBuildAndThrow(run, listener, CreateTag_Error_TagAttributesPath(), new IOException(e));
      }
    }

    if (isNotBlank(tagAttributesJson)) {
      try {
        tagAttributes.putAll(parseAttributes(tagAttributesJson));
      }
      catch (Exception e) {
        failBuildAndThrow(run, listener, CreateTag_Error_TagAttributesJson(), new IOException(e));
      }
    }

    try {
      String resolvedTagName = env.expand(tagName);
      Tag tag = client.createTag(resolvedTagName, tagAttributes);
      listener.getLogger().println("Successfully created tag: '" + tag.toJson() + "'");
    }
    catch (RepositoryManagerException e) {
      failBuildAndThrow(run, listener, e.getResponseMessage().orElse(e.getMessage()), new IOException(e));
    }
  }

  private Map<String, Object> parseAttributes(final FilePath workspace, final String filePath, final EnvVars env)
      throws IOException, InterruptedException
  {
    return parseAttributes(new FilePath(workspace, env.expand(filePath)).readToString());
  }

  private Map<String, Object> parseAttributes(String json) {
    return GSON.fromJson(json, ATTRIBUTE_TYPE);
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
  @Symbol("createTag")
  public static final class DescriptorImpl
      extends BuildStepDescriptor<Builder>
  {
    @Override
    public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return CreateTag_DisplayName();
    }

    public FormValidation doCheckNexusInstanceId(@QueryParameter String value) {
      return NxrmUtil.doCheckNexusInstanceId(value);
    }

    public ListBoxModel doFillNexusInstanceIdItems() {
      return NxrmUtil.doFillNexusInstanceIdItems(NEXUS_3);
    }

    public FormValidation doCheckTagName(@QueryParameter String tagName) {
      return FormUtil.validateNotEmpty(tagName, Common_Validation_Staging_TagNameRequired());
    }

    public FormValidation doCheckTagAttributesJson(@QueryParameter String tagAttributesJson) {
      if (!isBlank(tagAttributesJson)) {
        try {
          GSON.fromJson(tagAttributesJson, ATTRIBUTE_TYPE);
        }
        catch (Exception e) {
          return error(CreateTag_Validation_TagAttributesJson());
        }
      }

      return ok();
    }
  }
}
