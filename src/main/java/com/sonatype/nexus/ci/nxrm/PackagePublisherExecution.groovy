/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm

import javax.inject.Inject

import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution

class PackagePublisherExecution
    extends AbstractSynchronousNonBlockingStepExecution<Void>
{
  @Inject
  private transient NxrmPublisherWorkflowStep nxrmPublisher

  @Override
  protected Void run() throws Exception {
    // TODO Implement package publisher

    return null
  }
}
