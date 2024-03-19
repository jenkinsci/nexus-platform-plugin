package org.sonatype.nexus.ci.iq

import hudson.Extension
import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter

class CallflowRunConfiguration
    extends AbstractDescribableImpl<CallflowRunConfiguration>
{
  List<ScanPattern> callflowScanPatterns;

  List<String> callflowNamespaces;

  List<ScanPattern> getCallflowScanPatterns() {
    return callflowScanPatterns;
  }

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


  @Extension
  public static class DescriptorImpl extends Descriptor<CallflowRunConfiguration>
  {
    @Override
    String getDisplayName() {
      return Messages.CallflowRunConfiguration_DisplayName()
    }
  }
}
