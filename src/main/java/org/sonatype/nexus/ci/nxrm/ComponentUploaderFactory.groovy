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

import org.sonatype.nexus.ci.nxrm.v2.ComponentUploaderNxrm2
import org.sonatype.nexus.ci.nxrm.v3.ComponentUploaderNxrm3

import hudson.model.Run
import hudson.model.TaskListener
import hudson.util.FormValidation.Kind

import static hudson.model.Result.FAILURE
import static org.sonatype.nexus.ci.config.GlobalNexusConfiguration.globalNexusConfiguration
import static org.sonatype.nexus.ci.config.NxrmVersion.NEXUS_3
import static org.sonatype.nexus.ci.util.FormUtil.validateUrl

class ComponentUploaderFactory
{
  static ComponentUploader getComponentUploader(final String nexusInstanceId,
                                                final Run run,
                                                final TaskListener taskListener)
  {
    def logger = taskListener.getLogger()
    def nxrmConfig = globalNexusConfiguration.nxrmConfigs.find { it.id == nexusInstanceId }

    if (!nxrmConfig) {
      failRun(run, logger, "Nexus Configuration ${nexusInstanceId} not found.")
    }

    if (validateUrl(nxrmConfig.serverUrl).kind == Kind.ERROR) {
      failRun(run, logger, "Nexus Server URL ${nxrmConfig.serverUrl} is invalid.")
    }

    return nxrmConfig.version == NEXUS_3 ? new ComponentUploaderNxrm3(nxrmConfig, run, taskListener) :
        new ComponentUploaderNxrm2(nxrmConfig, run, taskListener)
  }

  private static void failRun(final Run run, final PrintStream logger, final String failMsg) {
    logger.println("Failing build due to: ${failMsg}")
    run.setResult(FAILURE)
    throw new IllegalArgumentException(failMsg)
  }
}
