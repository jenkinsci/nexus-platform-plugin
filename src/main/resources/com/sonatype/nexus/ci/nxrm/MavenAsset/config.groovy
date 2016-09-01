/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm.MavenAsset

def f = namespace(lib.FormTagLib)

f.section(title: descriptor.displayName) {
  f.entry(title: _('File Path'), field: 'filePath') {
    f.textbox(clazz: 'required')
  }

  f.entry(title: _('Classifier'), field: 'classifier') {
    f.textbox()
  }

  f.entry(title: _('Extension'), field: 'extension') {
    f.textbox()
  }
}
