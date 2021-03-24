package org.sonatype.nexus.ci.iq

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.NxiqConfiguration

import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import com.github.tomakehurst.wiremock.client.WireMock
import jenkins.model.Jenkins

import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat
import static com.github.tomakehurst.wiremock.client.WireMock.okJson
import static com.github.tomakehurst.wiremock.client.WireMock.post
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo

/**
 * Centralization of mocking out an IQ Server for basic responses.
 */
class IqServerMockUtility
{
  /**
   * Minimal IQ Server mock to allow plugin to succeed
   */
  static def configureIqServerMock(int port,
                                   String serverVersion = IqPolicyEvaluatorUtil.MINIMAL_SERVER_VERSION_REQUIRED)
  {
    WireMock.configureFor("localhost", port)
    givenThat(get(urlMatching('/rest/config/proprietary\\?.*'))
        .willReturn(okJson('{}')))
    givenThat(post(urlMatching('/rest/integration/applications/verifyOrCreate/.*'))
        .willReturn(okJson('true')))
    givenThat(post(urlMatching('/rest/integration/applications/app/evaluations/ci/stages/.*'))
        .willReturn(okJson('{"statusId": "statusId"}')))
    givenThat(get(urlMatching('/rest/integration/applications/app/evaluations/status/statusId'))
        .willReturn(okJson('''{
        "status": "COMPLETED",
        "reason": null,
        "result": {
        "alerts": [],
        "affectedComponentCount": 33,
        "criticalPolicyViolationCount": 20,
        "severePolicyViolationCount": 12,
        "moderatePolicyViolationCount": 1,
        "criticalPolicyViolationCount": 46,
        "severePolicyViolationCount": 54,
        "moderatePolicyViolationCount": 3,
        "grandfatheredPolicyViolationCount": 0
        },
        "scanReceipt": {
        "scanId": "scanId",
        "timeToReport": 6,
        "reportUrl": "ui/links/application/app/report/scanId",
        "pdfUrl": "ui/links/application/app/report/scanId/pdf",
        "dataUrl": "api/v2/applications/app/reports/scanId/raw",
        "reportTimeoutInSeconds": 350
        },
        "nextPollingIntervalInSeconds": 0
        }''')))
    givenThat(get(urlMatching('/rest/product/version'))
        .willReturn(okJson("""{"tag": "1e64d778447fc30e4f509f9ca965c5bbe7aa8fd3",
        "version": "${serverVersion}",
        "name": "sonatype-clm-server",
        "timestamp": "201807111516",
        "build": "build-number"}""")))
    givenThat(post(urlMatching('/api/v2/sourceControl.*'))
        .willReturn(okJson("""{"stageTypeId": "stageTypeId",
            "ownerId" : "",
            "repositoryUrl": "",
            "token": "",
            "provider": ""
            }""")))
    givenThat(get(urlPathEqualTo('/rest/policy/stages'))
        .willReturn(okJson('[{"stageTypeId":"develop","stageName":"Develop"},{"stageTypeId":"build","stageName":"Build"},{"stageTypeId":"stage-release","stageName":"Stage Release"},{"stageTypeId":"release","stageName":"Release"},{"stageTypeId":"operate","stageName":"Operate"}]'))
    )
  }

  /**
   * Configure Jenkins with Credentials for IQ Server and utilize those in the global IQ configuration.
   */
  static def configureJenkins(Jenkins jenkins, int port, boolean hideReports = false) {
    def nxiqConfiguration = [new NxiqConfiguration("http://localhost:$port", 'cred-id', hideReports)]
    GlobalNexusConfiguration.globalNexusConfiguration.iqConfigs = nxiqConfiguration
    GlobalNexusConfiguration.globalNexusConfiguration.nxrmConfigs = []
    def credentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, 'cred-id', 'name', 'user',
        'password')
    CredentialsProvider.lookupStores(jenkins).first().addCredentials(Domain.global(), credentials)
  }
}
