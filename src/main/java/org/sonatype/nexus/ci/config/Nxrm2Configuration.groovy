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
package org.sonatype.nexus.ci.config

import com.sonatype.nexus.api.exception.RepositoryManagerException

import org.sonatype.nexus.ci.config.NxrmConfiguration.NxrmDescriptor

import hudson.Extension
import hudson.util.FormValidation
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

import static hudson.util.FormValidation.error
import static hudson.util.FormValidation.ok
import static org.sonatype.nexus.ci.config.NxrmVersion.NEXUS_2
import static org.sonatype.nexus.ci.util.Nxrm2Util.getApplicableRepositories

class Nxrm2Configuration
    extends NxrmConfiguration
{
  @DataBoundConstructor
  Nxrm2Configuration(final String id,
                     final String internalId,
                     final String displayName,
                     final String serverUrl,
                     final String credentialsId)
  {
    super(id, internalId, displayName, serverUrl, credentialsId)
  }

  @Override
  NxrmVersion getVersion() {
    NEXUS_2
  }

  @Extension
  static class DescriptorImpl
      extends NxrmDescriptor
  {
    DescriptorImpl() {
      super(Nxrm2Configuration)
    }

    @Override
    String getDisplayName() {
      return 'Nexus Repository Manager 2.x Server'
    }

    @Override
    FormValidation doVerifyCredentials(@QueryParameter String serverUrl, @QueryParameter String credentialsId)
        throws IOException
    {
      try {
        def repositories = getApplicableRepositories(serverUrl, credentialsId)

        return ok(
            "Nexus Repository Manager 2.x connection succeeded (${repositories.size()} hosted release Maven 2 " +
                'repositories)')
      }
      catch (RepositoryManagerException e) {
        return error(e, 'Nexus Repository Manager 2.x connection failed')
      }
    }
  }
}
