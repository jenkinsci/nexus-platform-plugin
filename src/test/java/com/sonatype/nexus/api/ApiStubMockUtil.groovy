/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.api

import com.sonatype.nexus.api.ApiStub.NexusClientFactory
import com.sonatype.nexus.api.ApiStub.NxrmClient

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify
import static org.powermock.api.mockito.PowerMockito.mockStatic
import static org.powermock.api.mockito.PowerMockito.verifyStatic
import static org.powermock.api.mockito.PowerMockito.when

class ApiStubMockUtil
{
  /**
   * Mocks {@link NxrmClient#getNxrmRepositories()} with a default serverUrl, crednetialsId and repository list. Runs
   * the provided test closure. Verifies that the client was created and getNxrmRepositories was called.
   *
   * @param test The test closure will be called with three parameters: serverUrl, credentialsId, repositories
   */
  static void mockGetNxrmRepositoriesVerify(Closure test) {
    def serverUrl = 'http://foo.com'
    def credentialsId = 'barCredentialsId'
    def repositories = [
        [
            id: 'maven-releases'
        ],
        [
            id: 'nuget-releases'
        ]
    ]

    mockGetNxrmRepositoriesVerify(serverUrl, credentialsId, repositories,
        test.curry(serverUrl, credentialsId, repositories))
  }

  /**
   * Mocks {@link NxrmClient#getNxrmRepositories()}. Runs the provided test closure. Verifies that the client was
   * created and getNxrmRepositories was called.
   *
   * @param test The test closure that will be called
   */
  static void mockGetNxrmRepositoriesVerify(final String serverUrl, final String credentialsId, List repositories,
                                            Closure test)
  {
    mockBuildRmClientVerify(serverUrl, credentialsId, { NxrmClient nxrmClient ->
      when(nxrmClient.getNxrmRepositories()).thenReturn(repositories)
      test()
      verify(nxrmClient).getNxrmRepositories()
    })
  }

  /**
   * Mocks {@link NexusClientFactory#buildRmClient(String serverUrl, String credentialsId)}. Runs the provided test
   * closure. Verifies that the client was created.
   *
   * @param test The test closure will be called with one parameter, the generated {@link NxrmClient}
   */
  static void mockBuildRmClientVerify(final String serverUrl, final String credentialsId, Closure test) {
    def nxrmClient = mock(NxrmClient.class)

    mockStatic(NexusClientFactory.class)
    when(NexusClientFactory.buildRmClient(serverUrl, credentialsId)).thenReturn(nxrmClient)

    test(nxrmClient)

    verifyStatic()
    NexusClientFactory.buildRmClient(serverUrl, credentialsId)
  }
}
