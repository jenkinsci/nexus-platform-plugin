/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.config.NxiqConfiguration

import com.sonatype.nexus.ci.config.NxiqConfiguration
import com.sonatype.nexus.ci.config.Messages

def f = namespace(lib.FormTagLib)
def c = namespace(lib.CredentialsTagLib)

def nxiqConfiguration = (NxiqConfiguration) instance

f.section(title: descriptor.displayName) {
  f.entry(title: _(Messages.Configuration_ServerUrl()), field: 'serverUrl') {
    f.textbox(clazz: 'required')
  }

//  TODO INT-134 - PKI Authentication support needed in nexus-java-api
//  f.radioBlock(
//      name: _('isPkiAuthentication'),
//      title: _(Messages.Configuration_PKIAuthentication()),
//      inline: true,
//      value: true,
//      checked: nxiqConfiguration?.@isPkiAuthentication,
//      help: descriptor.getHelpFile('isPkiAuthentication')
//  ) {
//    f.block() {
//      f.validateButton(
//          title: _(Messages.Configuration_TestConnection()),
//          progress: _('Testing...'),
//          method: 'verifyCredentials',
//          with: 'serverUrl'
//      )
//    }
//  }

//  f.radioBlock(
//      name: _('isPkiAuthentication'),
//      title: _(Messages.Configuration_UserAuthentication()),
//      inline: true,
//      value: false,
//      checked: !nxiqConfiguration?.@isPkiAuthentication,
//      help: descriptor.getHelpFile('isNotPkiAuthentication')
//  ) {
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
//  }
}
