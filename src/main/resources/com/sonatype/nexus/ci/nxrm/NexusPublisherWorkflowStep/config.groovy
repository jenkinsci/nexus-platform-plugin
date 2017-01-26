/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm.NxrmPublisherBuildStep

import com.sonatype.nexus.ci.util.NxrmUtil

def f = namespace(lib.FormTagLib)
def l = namespace(lib.LayoutTagLib)

l.css(src: '/plugin/nexus-jenkins-plugin/css/nexus.css')

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
