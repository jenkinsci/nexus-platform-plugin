package org.sonatype.nexus.ci.nxrm

import com.sonatype.nexus.api.exception.RepositoryManagerException

interface ComponentUploader
{
  void uploadComponents(final String repository, final List<Package> packages) throws RepositoryManagerException
}
