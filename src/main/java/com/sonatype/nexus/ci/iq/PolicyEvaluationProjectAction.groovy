/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.iq

import hudson.model.Action
import hudson.model.Job

class PolicyEvaluationProjectAction
  implements Action
{
  private final Job job

  PolicyEvaluationProjectAction(Job job) {
    this.job = job
  }

  Job getJob() {
    return job
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
