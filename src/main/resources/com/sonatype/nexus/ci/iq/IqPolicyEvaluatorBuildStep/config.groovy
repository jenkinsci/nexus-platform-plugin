/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq.IqPolicyEvaluatorBuildStep

import com.sonatype.nexus.ci.config.NxiqConfiguration

def f = namespace(lib.FormTagLib)
def c = namespace(lib.CredentialsTagLib)
def l = namespace(lib.LayoutTagLib)

l.css(src: '/plugin/nexus-jenkins-plugin/css/nexus.css')

f.section(title: descriptor.displayName) {
  if (!NxiqConfiguration.iqConfig) {
    tr {
      td(class: 'setting-leftspace') {

      }
      td {

      }
      td(class: 'nexus-jenkins-error') {
        h3('No IQ Server configured.')
        div {
          yield "Add IQ Servers via: "
          a(href: jenkins.model.Jenkins.instance.rootUrl + "/configure", "Configure System")
        }
      }
    }
  }

  f.entry(title: _('Stage'), field: 'iqStage') {
    f.select()
  }

  f.entry(title: _('Application'), field: 'iqApplication') {
    f.select()
  }

  f.advanced() {
    f.section(title: _('Advanced options')) {
      f.entry(title: _('Scan files in workspace. Wildcards allowed e.g: **/*.jar'),
          help: '/plugin/nexus-jenkins-plugin/help-iqScanPatterns.html') {
        f.repeatable(field: 'iqScanPatterns', minimum: '0') {
          f.textbox(field: 'scanPattern')
          f.repeatableDeleteButton()
        }
      }

      f.entry(title: _('Fail build when unable to communicate with IQ Server'), field: 'failBuildOnNetworkError') {
        f.checkbox()
      }

      f.entry(title: _('Use job specific credentials'), field: 'jobCredentialsId') {
        c.select()
      }
    }
  }
}
