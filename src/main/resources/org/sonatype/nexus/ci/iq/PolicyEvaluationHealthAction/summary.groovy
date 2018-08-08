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
package org.sonatype.nexus.ci.iq.PolicyEvaluationHealthAction

import org.sonatype.nexus.ci.iq.Messages
import org.sonatype.nexus.ci.iq.PolicyEvaluationHealthAction

def t = namespace(lib.JenkinsTagLib)

def action = (PolicyEvaluationHealthAction) it

t.summary(icon: '/plugin/nexus-jenkins-plugin/images/48x48/nexus-iq.png') {
  // Inline the iq-chiclet css here for Jenkins v1 which does not support the css tag.
  style(type: 'text/css', """
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
  a(href: "${action.getUrlName()}", Messages.IqPolicyEvaluation_ReportName())
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
  span(Messages.IqPolicyEvaluation_NumberGrandfathered(action.grandfatheredPolicyViolationCount))
}
