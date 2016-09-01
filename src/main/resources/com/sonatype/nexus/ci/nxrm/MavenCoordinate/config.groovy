/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm.MavenCoordinate

import com.sonatype.nexus.api.repository.MavenPackageType

def f = namespace(lib.FormTagLib)

f.entry(title: _('Group'), field: 'groupId') {
  f.textbox(clazz: 'required')
}

f.entry(title: _('Artifact'), field: 'artifactId') {
  f.textbox(clazz: 'required')
}

f.entry(title: _('Version'), field: 'version') {
  f.textbox(clazz: 'required')
}

f.entry(title: _('Packaging'), field: 'packaging') {
  f.editableComboBox(clazz: 'required', field: 'packaging', items: MavenPackageType.corePackageTypes)
}
