package org.sonatype.nexus.ci.iq

import hudson.Extension
import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter

class CallflowRunConfiguration
    extends AbstractDescribableImpl<CallflowRunConfiguration>
{
  List<ScanPattern> callflowScanPatterns

  List<String> callflowNamespaces

  // This is used for experimental configuration. The options will not be widely publicised but can allow us to
  // internally test different callflow options
  Properties additionalConfiguration

  @DataBoundConstructor
  CallflowRunConfiguration(
      final List<ScanPattern> callflowScanPatterns,
      final List<String> callflowNamespaces = null,
      final Properties additionalConfiguration = null
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

  Properties getAdditionalConfiguration() {
    return this.additionalConfiguration
  }

  @DataBoundSetter
  void setAdditionalConfiguration(final Map<String, Object> additionalConfiguration) {
    this.additionalConfiguration = additionalConfiguration
  }

  @Extension
  static class DescriptorImpl extends Descriptor<CallflowRunConfiguration>
  {
    @Override
    String getDisplayName() {
      return Messages.IqPolicyEvaluation_callflowRunConfiguration()
    }
  }
}
