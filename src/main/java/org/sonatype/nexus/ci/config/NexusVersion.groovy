package org.sonatype.nexus.ci.config

enum NexusVersion
{
  UNKNOWN('Unknown'),
  NEXUS2(NX2),
  NEXUS3(NX3)

  private static final String NX2 = 'Nexus 2'
  private static final String NX3 = 'Nexus 3'

  String displayName

  NexusVersion(String displayName) {
    this.displayName = displayName
  }

  String getDisplayName() {
    displayName
  }

  static NexusVersion parse(String name) {
    if (name == null) {
      return UNKNOWN
    }

    switch (name) {
      case NX2:
        return NEXUS2
      case NX3:
        return NEXUS3
      default:
        try {
          return valueOf(name)
        }
        catch (IllegalArgumentException) {
          return UNKNOWN
        }
    }
  }
}
