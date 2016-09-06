/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm.NxrmPublisherBuildStep

def f = namespace(lib.FormTagLib)

f.section(title: descriptor.displayName) {
  f.entry(title: _('Nexus Instance'), field: 'nexusInstanceId') {
    f.select()
  }

  f.entry(title: _('Nexus Repository'), field: 'nexusRepositoryId') {
    f.select()
  }

  f.entry(title: _('Packages')) {
    f.repeatableHeteroProperty(
        field: 'packages',
        addCaption: _('Add Package')
    )
  }
}
