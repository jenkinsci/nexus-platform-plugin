/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm.NxrmPublisherBuildStep

import com.sonatype.nexus.ci.util.NxrmUtil

def f = namespace(lib.FormTagLib)

f.section(title: descriptor.displayName) {
  if (!NxrmUtil.hasNexusRepositoryManagerConfiguration()) {
    tr {
      td(class: 'setting-leftspace') {

      }
      td {

      }
      td(style: '''background: #faf3d1;
          border: 1px solid #eac9a9;
          -moz-border-radius: 3px;
          -webkit-border-radius: 3px;
          border-radius: px;
          padding: 0;
          -moz-box-shadow: 0 0 5px #ccc8b3;
          -webkit-box-shadow: 0 0 5px #ccc8b3;
          box-shadow: 0 0 5px #ccc8b3;
          margin: 15px;
          letter-spacing: normal;
          text-align: center''') {
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
