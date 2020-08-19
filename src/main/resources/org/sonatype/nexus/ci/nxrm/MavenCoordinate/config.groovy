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
package org.sonatype.nexus.ci.nxrm.MavenCoordinate

import com.sonatype.nexus.api.repository.v2.MavenPackageType

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
