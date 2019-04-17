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
package org.sonatype.nexus.ci.config.GlobalNexusConfiguration

import org.sonatype.nexus.ci.config.NxiqConfiguration

def f = namespace(lib.FormTagLib)
def iqConfig = NxiqConfiguration.iqConfig

f.section(title: descriptor.displayName) {
  f.entry(title: _('Nexus Repository Manager Servers')) {
    f.repeatableHeteroProperty(
        field: 'nxrmConfigs',
        addCaption: _('Add Nexus Repository Manager Server')
    )
  }

  f.entry(title: _('Nexus IQ Server')) {
    f.repeatableHeteroProperty(
        field: 'iqConfigs',
        addCaption: _('Add Nexus IQ Server'),
        oneEach: 'true'
    )
  }

  if (!iqConfig) {
    f.block() {
      f.checkbox(field: "hideNvsMessage", title: "Hide messages about what's coming to the Nexus Platform Plugin")
    }
  }
}
