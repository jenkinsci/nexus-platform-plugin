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
