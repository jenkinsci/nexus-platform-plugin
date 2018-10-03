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

import java.util.logging.Level

import com.sonatype.nexus.api.exception.RepositoryManagerException

import groovy.util.logging.Log
import hudson.Extension
import hudson.util.FormValidation
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

import static hudson.util.FormValidation.Kind.OK
import static hudson.util.FormValidation.error
import static hudson.util.FormValidation.ok
import static hudson.util.FormValidation.warning
import static org.sonatype.nexus.ci.config.NxrmConfiguration.NxrmDescriptor
import static org.sonatype.nexus.ci.config.NxrmVersion.NEXUS_3
import static org.sonatype.nexus.ci.util.Nxrm3Util.getApplicableRepositories

@Log
class Nxrm3Configuration
    extends NxrmConfiguration
{
  private static final int MAJOR_VERSION_REQ = 3

  private static final int MINOR_VERSION_REQ = 13

  private static final int PATCH_VERSION_REQ = 0

  private static final String DOT = '.'

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
      try {
        def repositories = getApplicableRepositories(serverUrl, credentialsId, 'maven2')
        ok("Nexus Repository Manager 3.x connection succeeded (${repositories.size()} hosted maven2 repositories)")
      }
      catch (RepositoryManagerException e) {
        error(e, 'Nexus Repository Manager 3.x connection failed')
      }
    }

    @SuppressWarnings('CatchException')
    @Override
    FormValidation doCheckServerUrl(@QueryParameter String value) {
      def nxrmUrl = value
      def validation = super.doCheckServerUrl(value)

      if (validation.kind != OK) {
        return validation
      }

      // check nexus version, warn if < 3.13.0 PRO
      try {
        def statusServiceUrl = "${nxrmUrl}${nxrmUrl.endsWith('/') ? '' : '/'}service/rest/wonderland/status"
        def response = new XmlSlurper().parseText(new URL(statusServiceUrl).text)
        def edition = response.edition.text()
        def version = response.version.text()
        def (major, minor) = version.tokenize(DOT).take(2).collect { it as int }

        if (!edition.equalsIgnoreCase('pro') || major < MAJOR_VERSION_REQ || minor < MINOR_VERSION_REQ) {
          return warning(
              "NXRM ${edition} ${version} found. Some operations require a Nexus Repository Manager Professional " +
                  "server version ${[MAJOR_VERSION_REQ, MINOR_VERSION_REQ, PATCH_VERSION_REQ].join(DOT)} or newer; " +
                  "use of an incompatible server will result in failed builds.")
        }
      }
      catch (Exception e) {
        log.log(Level.WARNING, "Unsuccessful request to ${nxrmUrl} for version information for compatibility check", e)

        return warning(
            'Unable to determine Nexus Repository Manager version. Certain operations may not be compatible with your' +
                ' server which could result in failed builds.')
      }

      ok()
    }
  }
}
