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

import groovy.util.logging.Log
import hudson.Extension
import hudson.util.FormValidation
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

import static hudson.util.FormValidation.error
import static hudson.util.FormValidation.ok
import static hudson.util.FormValidation.warning
import static java.util.logging.Level.WARNING
import static org.apache.commons.lang.SystemUtils.LINE_SEPARATOR
import static org.sonatype.nexus.ci.config.NxrmConfiguration.NxrmDescriptor
import static org.sonatype.nexus.ci.config.NxrmVersion.NEXUS_3
import static org.sonatype.nexus.ci.util.Nxrm3Util.getApplicableRepositories
import static org.sonatype.nexus.ci.util.RepositoryManagerClientUtil.nexus3Client

@Log
class Nxrm3Configuration
    extends NxrmConfiguration
{
  private static final int MAJOR_VERSION_REQ = 3

  private static final int MINOR_VERSION_REQ = 13

  private static final int PATCH_VERSION_REQ = 0

  private static final String DOT = '.'

  private static final String INVALID_VERSION_WARNING = "Some operations require Nexus Repository Manager " +
      "Professional server version ${MAJOR_VERSION_REQ}.${MINOR_VERSION_REQ}.${PATCH_VERSION_REQ} or " +
      "newer; use of an incompatible server could result in failed builds."

  private static final String CONNECTION_SUCCEEDED = 'Nexus Repository Manager 3.x connection succeeded'

  private static final String CONNECTION_FAILED = 'Nexus Repository Manager 3.x connection failed'

  @SuppressWarnings('ParameterCount')
  @DataBoundConstructor
  Nxrm3Configuration(final String id,
                     final String internalId,
                     final String displayName,
                     final String serverUrl,
                     final String credentialsId)
  {
    super(id, internalId, displayName, serverUrl, credentialsId)
  }

  @Override
  NxrmVersion getVersion() {
    NEXUS_3
  }

  @Extension
  static class DescriptorImpl
      extends NxrmDescriptor
  {
    DescriptorImpl() {
      super(Nxrm3Configuration)
    }

    @Override
    String getDisplayName() {
      return 'Nexus Repository Manager 3.x Server'
    }

    @Override
    FormValidation doVerifyCredentials(@QueryParameter String serverUrl, @QueryParameter String credentialsId)
        throws IOException
    {
      def repositories
      def badVersionMsg = ''

      try {
        // check nexus version, warn if < 3.13.0 PRO
        def client = nexus3Client(serverUrl, credentialsId)
        def sv = client.getVersion()
        def (major, minor) = sv.version.tokenize(DOT).take(2).collect { it as int }

        if (!sv.edition.equalsIgnoreCase('pro') || major < MAJOR_VERSION_REQ || minor < MINOR_VERSION_REQ) {
          badVersionMsg = "NXRM ${sv.edition} ${sv.version} found."
        }
      }
      catch (RepositoryManagerException e) {
        log.log(WARNING, "Unsuccessful request to ${serverUrl} for version information for compatibility check", e)
        return error(e, CONNECTION_FAILED)
      }

      if (badVersionMsg) {
        warning(CONNECTION_SUCCEEDED +
                "${LINE_SEPARATOR}${LINE_SEPARATOR} ${badVersionMsg} ${INVALID_VERSION_WARNING}")
      }
      else {
        try {
          repositories = getApplicableRepositories(serverUrl, credentialsId, 'maven2')
          ok(CONNECTION_SUCCEEDED + " (${repositories.size()} hosted maven2 repositories)")
        }
        catch (RepositoryManagerException e) {
          return error(e, CONNECTION_FAILED)
        }
      }
    }
  }
}
