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
package org.sonatype.nexus.ci.config.NxiqConfiguration

import org.sonatype.nexus.ci.config.NxiqConfiguration
import org.sonatype.nexus.ci.config.Messages

def f = namespace(lib.FormTagLib)
def c = namespace(lib.CredentialsTagLib)

def nxiqConfiguration = (NxiqConfiguration) instance

f.section(title: descriptor.displayName) {
  f.invisibleEntry() {
    // When instance is null, new server configuration so generate new internalId
    input(type: 'hidden', name: 'internalId',
        value: "${instance != null ? instance.internalId : UUID.randomUUID().toString()}")
  }

  f.entry(title: _(Messages.Configuration_ServerID()), field:'id') {
    f.textbox()
  }

  f.entry(title: _(Messages.Configuration_ServerUrl()), field: 'serverUrl') {
    f.textbox(clazz: 'required')
  }

  f.entry(title: _(Messages.Configuration_Credentials()), field: 'credentialsId') {
    c.select(context:app, includeUser:false, expressionAllowed:false)
  }

  f.block() {
    f.validateButton(
        title: _(Messages.Configuration_TestConnection()),
        progress: _('Testing...'),
        method: 'verifyCredentials',
        with: 'serverUrl,credentialsId'
    )
  }
}
