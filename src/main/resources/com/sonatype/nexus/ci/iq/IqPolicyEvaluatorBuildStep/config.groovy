/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq.IqPolicyEvaluatorBuildStep

import com.sonatype.nexus.ci.config.NxiqConfiguration
import com.sonatype.nexus.ci.iq.Messages

def f = namespace(lib.FormTagLib)
def c = namespace(lib.CredentialsTagLib)
def l = namespace(lib.LayoutTagLib)

def nxiqConfiguration = NxiqConfiguration.iqConfig

l.css(src: "${rootURL}/plugin/nexus-jenkins-plugin/css/nexus.css")

f.section(title: descriptor.displayName) {
  if (!nxiqConfiguration) {
    tr {
      td(class: 'setting-leftspace') {

      }
      td {

      }
      td(class: 'nexus-jenkins-error') {
        h3(Messages.IqPolicyEvaluation_NoIqServersConfigured())
        div {
          yield Messages.IqPolicyEvaluation_AddIqServers()
          a(href: jenkins.model.Jenkins.instance.rootUrl + "/configure", "Configure System")
        }
      }
    }
  }

  f.entry(title: _(Messages.IqPolicyEvaluation_Stage()), field: 'iqStage') {
    f.select()
  }

  f.entry(title: _(Messages.IqPolicyEvaluation_Application()), field: 'iqApplication') {
    f.select()
  }

  f.advanced() {
    f.section(title: _(Messages.IqPolicyEvaluation_AdvancedOptions())) {
      f.entry(title: _(Messages.IqPolicyEvaluation_ScanPatterns()),
          help: descriptor.getHelpFile('iqScanPatterns')) {
        f.repeatable(field: 'iqScanPatterns', minimum: '0') {
          f.textbox(field: 'scanPattern')
          f.repeatableDeleteButton()
        }
      }

      f.entry(title: _(Messages.IqPolicyEvaluation_FailOnNetwork()), field: 'failBuildOnNetworkError') {
        f.checkbox()
      }

      if (!nxiqConfiguration?.@isPkiAuthentication) {
        f.entry(title: _(Messages.IqPolicyEvaluation_JobSpecificCredentials()), field: 'jobCredentialsId') {
          c.select(context:app, includeUser:false, expressionAllowed:false)
        }
      } else {
        f.entry() {
          div(class: 'nexus-jenkins-error') {
            yield Messages.IqPolicyEvaluation_NoJobSpecificCredentials()
          }
        }
      }
    }
  }
}
