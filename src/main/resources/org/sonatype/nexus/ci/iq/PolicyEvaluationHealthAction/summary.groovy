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
import org.sonatype.nexus.ci.util.IqUtil

def t = namespace(lib.JenkinsTagLib)

def action = (PolicyEvaluationHealthAction) it

def policyCss = {
  // Inline the iq-chiclet css here for Jenkins v1 which does not support the css tag.
  style(type: 'text/css', """
        .iq-job-main-table {
          margin-top: 1em;
          margin-left: 1em;
        }
        
        .iq-job-main-table img {
          margin-right: 0.3em !important;
        }

        .iq-block {
          display:inline-block;
          margin-right: 1em;
          float: left;
          line-height: 1.4;
          margin-bottom: 1.5em;
        }
        
        .iq-title {
          font-weight:bold;
          padding-right: 0.25em;
        }
        
        .iq-label {
          font-weight: bold;
        }
        
        .p-iq-chiclet {
          margin: 3px 0;
          padding-left: 0;
        }
        
        .iq-chiclet {
          display:inline-block;
          width:35px;
          text-align:center;
          border-radius:5px;
          -moz-border-radius:5px;
          color:white;
          margin-right: 3px;
          padding-right: 1px;
          font-weight: bold;
          padding-top: 2px;
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
        
        .iq-chiclet-message {
          margin-left: 0.4em;
          padding-top: 2px;
        }
      """)
}

def policyUI = {
  div(class: 'iq-block') {
    div() {
      span(class: 'iq-title', Messages.IqPolicyEvaluation_ReportName())
      a(href: "${action.getUrlName()}", target: "_blank", "(view report)")
    }
    if (action.getIqDisplayName()) {
      div() {
        span(class: 'iq-label', Messages.IqPolicyEvaluation_IqDisplayNameLabel() + ' : ')
        span("${action.getIqDisplayName()}")
      }
    }
    if (action.getIqServerUrl()) {
      div() {
        span(class: 'iq-label', Messages.IqPolicyEvaluation_IqServerUrlLabel() + ' : ')
        span("${action.getIqServerUrl()}")
      }
    }
    if (action.getApplicationId()) {
      div() {
        span(class: 'iq-label', Messages.IqPolicyEvaluation_ApplicationLabel() + ' : ')
        span("${action.getApplicationId()}")
      }
      div() {
        span(class: 'iq-label', Messages.IqPolicyEvaluation_StageLabel() + ' : ')
        span("${action.getIqStage()}")
      }
    }

    if (action.totalPolicyViolationCount == 0 && action.affectedComponentCount > 0) {
      div(class: 'p-iq-chiclet') {
        span(class: 'iq-chiclet critical', action.criticalComponentCount ? action.criticalComponentCount : 0)
        span(class: 'iq-chiclet severe', action.severeComponentCount ? action.severeComponentCount : 0)
        span(class: 'iq-chiclet moderate', action.moderateComponentCount ? action.moderateComponentCount : 0)
        span(class: 'iq-chiclet-message',
            Messages.IqPolicyEvaluation_NumberGrandfathered(action.grandfatheredPolicyViolationCount))
      }
    }
    else {
      div(class: 'p-iq-chiclet') {
        span(class: 'iq-label', Messages.IqPolicyEvaluation_TotalViolations(action.totalPolicyViolationCount))
        span(class: 'iq-chiclet-message',
            Messages.IqPolicyEvaluation_AffectedComponents(action.affectedComponentCount))
      }
      div(class: 'p-iq-chiclet') {
        span(class: 'iq-chiclet critical',
            action.criticalPolicyViolationCount ? action.criticalPolicyViolationCount : 0)
        span(class: 'iq-chiclet severe', action.severePolicyViolationCount ? action.severePolicyViolationCount : 0)
        span(class: 'iq-chiclet moderate',
            action.moderatePolicyViolationCount ? action.moderatePolicyViolationCount : 0)
      }
      div(class: 'p-iq-chiclet') {
        span(Messages.IqPolicyEvaluation_NumberGrandfathered(action.grandfatheredPolicyViolationCount))
      }
    }
  }
}

t.summary(icon: '/plugin/nexus-jenkins-plugin/images/48x48/nexus-iq.png', policyUI << policyCss)
