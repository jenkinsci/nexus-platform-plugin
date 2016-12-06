/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.config.GlobalNexusConfiguration

def f = namespace(lib.FormTagLib)

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
}
