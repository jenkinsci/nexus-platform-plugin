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
package org.sonatype.nexus.ci.iq

import hudson.model.Action
import hudson.model.Job

class PolicyEvaluationProjectAction
  implements Action
{
  private final Job job

  private final String reportLink

  PolicyEvaluationProjectAction(Job job, String reportLink) {
    this.job = job
    this.reportLink = reportLink
  }

  Job getJob() {
    return job
  }

  String getReportLink() {
    return reportLink
  }

  @Override
  String getIconFileName() {
    // Returns null to prevent an entry in the Project's sidebar
    return null
  }

  @Override
  String getDisplayName() {
    // Returns null to prevent an entry in the Project's sidebar
    return null
  }

  @Override
  String getUrlName() {
    // Returns null to prevent an entry in the Project's sidebar
    return null
  }
}
