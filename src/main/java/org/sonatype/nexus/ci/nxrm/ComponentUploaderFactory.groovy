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

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener

import static hudson.model.Result.FAILURE
import static org.sonatype.nexus.ci.config.NexusVersion.NEXUS2
import static org.sonatype.nexus.ci.config.NexusVersion.NEXUS3
import static org.sonatype.nexus.ci.config.NexusVersion.UNKNOWN
import static org.sonatype.nexus.ci.util.NxrmUtil.getNexusConfiguration

class ComponentUploaderFactory
{
  static ComponentUploader getComponentUploader(final String nexusInstanceId,
                                                final Run run,
                                                final FilePath baseDir,
                                                final TaskListener taskListener)
  {
    def logger = taskListener.getLogger()
    def environment = run.getEnvironment(taskListener)
    def nexusConfiguration = getNexusConfiguration(nexusInstanceId)

    if (!nexusConfiguration) {
      illegalArg(run, logger, "Nexus Configuration ${nexusInstanceId} not found.",
          'Failing build due to missing Nexus Configuration')
    }

    switch (nexusConfiguration.nexusVersion) {
      case NEXUS2:
        return new org.sonatype.nexus.ci.nxrm.v2.ComponentUploaderImpl(nexusConfiguration, baseDir, environment, logger)
      case NEXUS3:
        return new org.sonatype.nexus.ci.nxrm.v3.ComponentUploaderImpl(nexusConfiguration, baseDir, environment, logger)
      case UNKNOWN:
      default:
        illegalArg(run, logger, "Nexus Configuration ${nexusInstanceId} has an unknown repository version.",
            'Failing build due to unknown Nexus Repository Manager version')
        break
    }
  }

  private static void illegalArg(run, logger, logMessage, failMessage) {
    logger.println(logMessage)
    logger.println(failMessage)
    run.setResult(FAILURE)
    throw new IllegalArgumentException(logMessage)
  }
}
