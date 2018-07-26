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
package org.sonatype.nexus.ci.nxrm.v3.AssociateTagStep

import org.sonatype.nexus.ci.util.NxrmUtil

def f = namespace(lib.FormTagLib)
def l = namespace(lib.LayoutTagLib)

l.css(src: "plugin/nexus-jenkins-plugin/css/nexus.css")

f.section() {
  if (!NxrmUtil.hasNexusRepositoryManagerConfiguration()) {
    tr {
      td(class: 'setting-leftspace') {}
      td {}
      td(class: 'nexus-jenkins-error') {
        h3('No Nexus Repository Manager configured.')
        div {
          yield "Add Nexus Repository Managers via: "
          a(href: jenkins.model.Jenkins.instance.rootUrl + "/configure", "Configure System")
        }
      }
    }
  }

  f.entry(title: _('Nexus Instance'), field: 'nexusInstanceId') {
    f.select()
  }

  f.entry(title: _('Tag Name'), field: 'tagName') {
    f.textbox(clazz: 'required')
  }

  f.entry(title: 'Search Criteria', field: 'search') {
    f.repeatableProperty(field: 'search', minimum: '1')
  }
}
