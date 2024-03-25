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

import hudson.Extension
import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter

class CallflowConfiguration
    extends AbstractDescribableImpl<CallflowConfiguration>
{
  List<ScanPattern> callflowScanPatterns

  List<String> callflowNamespaces

  // This is used for experimental configuration. The options will not be widely publicised but can allow us to
  // internally test different callflow options
  Map<String, Object> additionalConfiguration

  @DataBoundConstructor
  CallflowConfiguration(
      final List<ScanPattern> callflowScanPatterns = null,
      final List<String> callflowNamespaces = null,
      final Map<String, Object> additionalConfiguration = null
  ) {
    this.callflowScanPatterns = callflowScanPatterns
    this.callflowNamespaces = callflowNamespaces
    this.additionalConfiguration = additionalConfiguration
  }

  List<ScanPattern> getCallflowScanPatterns() {
    return callflowScanPatterns
  }

  @DataBoundSetter
  void setCallflowScanPatterns(final List<ScanPattern> callflowScanPatterns) {
    this.callflowScanPatterns = callflowScanPatterns
  }

  List<String> getCallflowNamespaces() {
    return callflowNamespaces
  }

  @DataBoundSetter
  void setCallflowNamespaces(final List<String> callflowNamespaces) {
    this.callflowNamespaces = callflowNamespaces
  }

  Map<String, Object> getAdditionalConfiguration() {
    return this.additionalConfiguration
  }

  @DataBoundSetter
  void setAdditionalConfiguration(final Map<String, Object> additionalConfiguration) {
    this.additionalConfiguration = additionalConfiguration
  }

  @Extension
  static class DescriptorImpl extends Descriptor<CallflowConfiguration>
  {
    @Override
    String getDisplayName() {
      return Messages.IqPolicyEvaluation_callflowConfiguration()
    }
  }
}
