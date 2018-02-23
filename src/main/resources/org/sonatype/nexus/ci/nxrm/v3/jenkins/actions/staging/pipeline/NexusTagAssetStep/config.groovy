package org.sonatype.nexus.ci.nxrm.v3.jenkins.actions.staging.pipeline.NexusTagAssetStep

import org.sonatype.nexus.ci.util.NxrmUtil

def f = namespace(lib.FormTagLib)
def l = namespace(lib.LayoutTagLib)

l.css(src: "${rootURL}/plugin/nexus-jenkins-plugin/css/nexus.css")

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

  f.entry(title: _('Tag Name'), field: 'tagName') {
    f.textbox()
  }

  f.entry(title: _('Assets to Tag'), field: 'includes') {
    f.textbox()
  }
}
