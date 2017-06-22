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
package org.sonatype.nexus.ci.iq.IqPolicyEvaluatorBuildStep

import org.sonatype.nexus.ci.config.NxiqConfiguration
import org.sonatype.nexus.ci.iq.Messages

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

      f.entry(title: _(Messages.IqPolicyEvaluation_JobSpecificCredentials()), field: 'jobCredentialsId') {
        c.select(context:app, includeUser:false, expressionAllowed:false)
      }
    }
  }
}
