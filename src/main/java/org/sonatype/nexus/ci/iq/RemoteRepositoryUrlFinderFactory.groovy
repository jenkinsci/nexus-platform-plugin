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

import hudson.FilePath
import org.slf4j.Logger

class RemoteRepositoryUrlFinderFactory
{
  static RemoteRepositoryUrlFinder getRepositoryUrlFinder(final FilePath workspace,
                                                          final Logger log,
                                                          final String instanceId,
                                                          final applicationId,
                                                          final Map<String, String> envVars)
  {
    new RemoteRepositoryUrlFinder(workspace, log, instanceId, applicationId, envVars)
  }
}
