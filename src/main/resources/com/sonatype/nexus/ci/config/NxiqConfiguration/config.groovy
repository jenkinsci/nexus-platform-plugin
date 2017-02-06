/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.config.NxiqConfiguration

import com.sonatype.nexus.ci.config.NxiqConfiguration

def f = namespace(lib.FormTagLib)
def c = namespace(lib.CredentialsTagLib)

def nxiqConfiguration = (NxiqConfiguration) instance

f.section(title: descriptor.displayName) {
  f.entry(title: _('Server URL'), field: 'serverUrl') {
    f.textbox(clazz: 'required')
  }

  f.radioBlock(
      name: _('isPkiAuthentication'),
      title: _('PKI Authentication'),
      inline: true,
      value: true,
      checked: nxiqConfiguration?.@isPkiAuthentication,
      help: descriptor.getHelpFile('isPkiAuthentication')
  ) {
    f.block() {
      f.validateButton(
          title: _('Test connection'),
          progress: _('Testing...'),
          method: 'verifyCredentials',
          with: 'serverUrl'
      )
    }
  }

  f.radioBlock(
      name: _('isPkiAuthentication'),
      title: _('User Authentication'),
      inline: true,
      value: false,
      checked: !nxiqConfiguration?.@isPkiAuthentication,
      help: descriptor.getHelpFile('isNotPkiAuthentication')
  ) {
    f.entry(title: _('Credentials'), field: 'credentialsId') {
      c.select()
    }

    f.block() {
      f.validateButton(
          title: _('Test connection'),
          progress: _('Testing...'),
          method: 'verifyCredentials',
          with: 'serverUrl,credentialsId'
      )
    }
  }
}
