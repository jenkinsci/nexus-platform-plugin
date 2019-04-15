/*
 * Copyright (c) 2019-present Sonatype, Inc. All rights reserved.
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
package org.sonatype.nexus.ci.nvs

import javax.annotation.Nonnull

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.NxiqConfiguration

import hudson.Extension
import hudson.model.Action
import hudson.model.Job
import jenkins.model.TransientActionFactory

@Extension
class NvsMessageActionFactory
    extends TransientActionFactory<Job>
{
  @Override
  Class<Job> type() {
    return Job.class
  }

  @Nonnull
  @Override
  Collection<? extends Action> createFor(Job target) {
    def isIqConfigured = NxiqConfiguration.getIqConfig() != null
    def hideNvsMessage = GlobalNexusConfiguration.globalNexusConfiguration?.hideNvsMessage
    if (isIqConfigured || hideNvsMessage) {
      return Collections.emptyList()
    }
    return Collections.singleton(new NvsMessageAction())
  }
}
