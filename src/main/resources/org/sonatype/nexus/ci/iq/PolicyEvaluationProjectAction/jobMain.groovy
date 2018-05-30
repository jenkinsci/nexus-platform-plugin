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
package org.sonatype.nexus.ci.iq.PolicyEvaluationProjectAction

import org.sonatype.nexus.ci.iq.Messages
import org.sonatype.nexus.ci.iq.PolicyEvaluationHealthAction
import org.sonatype.nexus.ci.iq.PolicyEvaluationProjectAction

def t = namespace(lib.JenkinsTagLib)

def projectAction = (PolicyEvaluationProjectAction) it
def action = projectAction.getJob().lastCompletedBuild.getAction(PolicyEvaluationHealthAction.class)

if (action) {
  table(class: 'iq-job-main-table') {
    t.summary(icon: '/plugin/nexus-jenkins-plugin/images/48x48/nexus-iq.png') {
      style(type: 'text/css', """
            .nexus-jenkins-error {
              background: #faf3d1;
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
              text-align: center
            }
            
            .iq-job-main-table {
              margin-top: 1em;
              margin-left: 1em;
            }
            
            .iq-job-main-table img {
              margin-right: 0 !important;
            }
            
            .iq-chiclet {
              display:inline-block;
              width:25px;
              text-align:center;
              border-radius:5px;
              -moz-border-radius:5px;
              color:white;
              margin-right: 5px;
            }
            
            .iq-chiclet.critical {
              background-color: #bc012f;
            }
            
            .iq-chiclet.severe {
              background-color: #f4861d;
            }
            
            .iq-chiclet.moderate {
              background-color: #f5c648;
            }
          """)
      a(href: "lastCompletedBuild/${action.getUrlName()}", Messages.IqPolicyEvaluation_LatestReportName())
      br()
      img(src: "${rootURL}/plugin/nexus-jenkins-plugin/images/16x16/governance-badge.png")
      if (action.criticalComponentCount) {
        span(class: 'iq-chiclet critical', action.criticalComponentCount)
      }
      if (action.severeComponentCount) {
        span(class: 'iq-chiclet severe', action.severeComponentCount)
      }
      if (action.moderateComponentCount) {
        span(class: 'iq-chiclet moderate', action.moderateComponentCount)
      }
      if (!action.criticalComponentCount && !action.severeComponentCount && !action.moderateComponentCount) {
        span(Messages.IqPolicyEvaluation_NoViolations())
      }
    }
  }
}
