/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.iq

class IqPolicyEvaluatorBuildStepTest
    extends IqPolicyEvaluatorDescriptorTest
{
  @Override
  IqPolicyEvaluatorDescriptor getDescriptor() {
    return (IqPolicyEvaluatorDescriptor) jenkins.getInstance().getDescriptor(IqPolicyEvaluatorBuildStep.class)
  }

  def 'it accepts all job types'() {
    setup:
      def descriptor = (IqPolicyEvaluatorBuildStep.DescriptorImpl)getDescriptor()

    expect:
      "job type is $jobType"
      expected == descriptor.isApplicable(jobType)

    where:
      jobType                             | expected
      hudson.model.FreeStyleProject.class | true
      Object                              | true
      null                                | true
  }
}
