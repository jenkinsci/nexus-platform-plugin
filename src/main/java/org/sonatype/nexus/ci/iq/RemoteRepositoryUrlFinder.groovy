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

import com.sonatype.nexus.git.utils.repository.RepositoryUrlFinderBuilder

import hudson.FilePath
import jenkins.security.MasterToSlaveCallable
import org.slf4j.Logger

class RemoteRepositoryUrlFinder
    extends MasterToSlaveCallable<String, RuntimeException>
{
  private final FilePath workspace

  private final Logger log

  private final String instanceId

  private final String applicationId

  private final Map<String, String> envVars

  RemoteRepositoryUrlFinder(final FilePath workspace,
                            final Logger log,
                            final String instanceId,
                            final applicationId,
                            final Map<String, String> envVars)
  {
    this.workspace = workspace
    this.log = log
    this.instanceId = instanceId
    this.applicationId = applicationId
    this.envVars = envVars
  }

  @Override
  @SuppressWarnings('CatchException')
  String call() {
    try {
      def workDirectory = new File(workspace.getRemote())
      final Optional optional = new RepositoryUrlFinderBuilder()
          .withEnvironmentVariableDefault()
          .withGitRepoAtPath(workDirectory == null ? null : workDirectory.getAbsolutePath() + '/.git')
          .withEnvironmentOverride(envVars)
          .withLogger(log)
          .build()
          .tryGetRepositoryUrl()
      if (optional.isPresent()) {
        def repoUrl = optional.get()
        log.info('Repository URL {} was found using automation', repoUrl)
        return repoUrl
      }
      else {
        log.debug('Repository URL for application with id: {} could not be found using automation', applicationId)
      }
    }
    catch (final Exception e) {
      log.debug('Could not find the repository URL due to: {}', e.getMessage())
    }
    return null
  }
}
