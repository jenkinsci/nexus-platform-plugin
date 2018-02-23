package org.sonatype.nexus.ci.nxrm.v3.jenkins.actions.staging

interface NexusTagAsset
{
  String getNexusInstanceId()

  String getTagName()

  String getIncludes()
}
