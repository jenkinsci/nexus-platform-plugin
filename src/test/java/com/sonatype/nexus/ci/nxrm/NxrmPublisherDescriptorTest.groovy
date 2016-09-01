package com.sonatype.nexus.ci.nxrm

import com.sonatype.nexus.ci.config.GlobalNexusConfiguration
import com.sonatype.nexus.ci.config.Nxrm2Configuration
import com.sonatype.nexus.ci.config.NxrmConfiguration
import com.sonatype.nexus.ci.util.FormUtil

import hudson.model.Describable
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.JenkinsRule

import static com.sonatype.nexus.api.ApiStubMockUtil.mockGetNxrmRepositoriesVerify
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasSize
import static org.junit.Assert.assertThat

abstract class NxrmPublisherDescriptorTest
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  private final Class<? extends Describable> describable

  NxrmPublisherDescriptorTest(Class<? extends Describable> describable) {
    this.describable = describable
  }

  @Test
  public void 'it populates Nexus instances'() {
    def nxrm2Configuration = saveGlobalConfigurationWithNxrm2Configuration()

    def configuration = (NxrmPublisherDescriptor) jenkins.getInstance().getDescriptor(describable)
    def listBoxModel = configuration.doFillNexusInstanceIdItems()

    assertThat(listBoxModel, hasSize(2))
    assertThat(listBoxModel.get(0).name, equalTo(FormUtil.EMPTY_LIST_BOX_NAME))
    assertThat(listBoxModel.get(0).value, equalTo(FormUtil.EMPTY_LIST_BOX_VALUE))
    assertThat(listBoxModel.get(1).name, equalTo(nxrm2Configuration.displayName))
    assertThat(listBoxModel.get(1).value, equalTo(nxrm2Configuration.internalId))
  }

  @Test
  public void 'it populates Nexus repositories'() {
    def nxrm2Configuration = saveGlobalConfigurationWithNxrm2Configuration()
    def repositories = [
        [
            id  : 'maven-releases',
            name: 'Maven Releases'
        ],
        [
            id  : 'nuget-releases',
            name: 'NuGet Releases'
        ]
    ]
    def configuration = (NxrmPublisherDescriptor) jenkins.getInstance().getDescriptor(describable)

    mockGetNxrmRepositoriesVerify(nxrm2Configuration.serverUrl, nxrm2Configuration.credentialsId, repositories, {
      def listBoxModel = configuration.doFillNexusRepositoryIdItems(nxrm2Configuration.internalId)

      assertThat(listBoxModel, hasSize(3))
      assertThat(listBoxModel.get(0).name, equalTo(FormUtil.EMPTY_LIST_BOX_NAME))
      assertThat(listBoxModel.get(0).value, equalTo(FormUtil.EMPTY_LIST_BOX_VALUE))
      assertThat(listBoxModel.get(1).name, equalTo(repositories.get(0).name))
      assertThat(listBoxModel.get(1).value, equalTo(repositories.get(0).id))
      assertThat(listBoxModel.get(2).name, equalTo(repositories.get(1).name))
      assertThat(listBoxModel.get(2).value, equalTo(repositories.get(1).id))
    })
  }

  protected Nxrm2Configuration saveGlobalConfigurationWithNxrm2Configuration() {
    def configurationList = new ArrayList<NxrmConfiguration>()
    def nxrm2Configuration = new Nxrm2Configuration('internalId', 'displayName', 'http://foo.com', 'credentialsId')
    configurationList.push(nxrm2Configuration)

    def globalConfiguration = jenkins.getInstance().getDescriptorByType(GlobalNexusConfiguration.class)
    globalConfiguration.nxrmConfigs = configurationList
    globalConfiguration.save()

    return nxrm2Configuration
  }
}
