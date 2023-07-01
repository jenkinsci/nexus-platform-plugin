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
package org.sonatype.nexus.ci.nxrm.NexusPublisherBuildStep

import org.sonatype.nexus.ci.nxrm.NexusPublisherBuildStep
import org.sonatype.nexus.ci.util.NxrmUtil

def f = namespace(lib.FormTagLib)
def l = namespace(lib.LayoutTagLib)
def st = namespace('jelly:stapler')

NexusPublisherBuildStep jobConfig = instance
def tagVisible = descriptor.getTagVisibility(jobConfig?.nexusInstanceId)

st.bind(var:'descriptor', value:descriptor)
script() {
  text('''
    function adjustTagVisibility(e) {
      descriptor.getTagVisibility(e.value, function(t) {
        var visibility =  t.responseObject();
        var tagName = findNextFormItem(e, "tagName");
        tagName.style.visibility = visibility;
        if (visibility === "hidden") {
          tagName.value = "";
        }
      });
    }
  ''')
}

link(rel: "stylesheet", href: "${rootURL}/plugin/nexus-jenkins-plugin/css/nexus.css", type: "text/css")

f.section(title: descriptor.displayName) {
  if (!NxrmUtil.hasNexusRepositoryManagerConfiguration()) {
    tr {
      td(class: 'setting-leftspace') {

      }
      td {

      }
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
    f.select(onchange: 'adjustTagVisibility(this)')
  }

  f.entry(title: _('Nexus Repository'), field: 'nexusRepositoryId') {
    f.select()
  }

  f.entry(title: _('Tag'), field: 'tagName') {
    f.textbox(style: "visibility: ${tagVisible};")
  }

  f.entry(title: _('Packages')) {
    f.repeatableHeteroProperty(
        field: 'packages',
        addCaption: _('Add Package')
    )
  }
}
