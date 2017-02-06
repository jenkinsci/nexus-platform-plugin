/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.config.Nxrm2Configuration

def f = namespace(lib.FormTagLib)
def c = namespace(lib.CredentialsTagLib)

f.section(title: descriptor.displayName) {
  f.invisibleEntry() {
    // When instance is null, new server configuration so generate new internalId
    input(type: 'hidden', name: 'internalId',
        value: "${instance != null ? instance.internalId : UUID.randomUUID().toString()}")
  }

  f.entry(title: _('Display Name'), field: 'displayName') {
    f.textbox(clazz: 'required')
  }

  f.entry(title: _('Server ID'), field:'id') {
    f.textbox(clazz: 'required')
  }

  f.entry(title: _('Server URL'), field: 'serverUrl') {
    f.textbox(clazz: 'required')
  }

  f.entry(title: _('Credentials'), field: 'credentialsId') {
    c.select(context:app, includeUser:false, expressionAllowed:false)
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
