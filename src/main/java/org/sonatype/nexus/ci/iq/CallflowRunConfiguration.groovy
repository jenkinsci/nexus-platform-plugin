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

  List<ScanPattern> getCallflowScanPatterns() {
    return callflowScanPatterns
  }

  Map<String, Object> additionalConfiguration;

  @DataBoundConstructor
  CallflowRunConfiguration(
      final List<ScanPattern> callflowScanPatterns,
      final List<String> callflowNamespaces
  ) {
    this.callflowScanPatterns = callflowScanPatterns
    this.callflowNamespaces = callflowNamespaces
  }

  @DataBoundSetter
  void setCallflowScanPatterns(final List<ScanPattern> callflowScanPatterns) {
    this.callflowScanPatterns = callflowScanPatterns
  }

  List<String> getCallflowNamespaces() {
    return callflowNamespaces;
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
  public static class DescriptorImpl extends Descriptor<CallflowRunConfiguration>
  {
    @Override
    String getDisplayName() {
      return Messages.IqPolicyEvaluation_callFlowOptions()
    }
  }
}
