/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm.MavenPackage

def f = namespace(lib.FormTagLib)

f.property(field: 'mavenCoordinate')

f.entry(title: _('Artifacts')) {
  f.repeatableHeteroProperty(
      field: 'mavenAssetList',
      addCaption: _('Add Artifact Path')
  )
}
