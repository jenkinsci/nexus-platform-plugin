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
package org.sonatype.nexus.ci.util

import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import hudson.model.Item
import hudson.model.Project
import hudson.model.User
import hudson.security.ACL
import hudson.security.GlobalMatrixAuthorizationStrategy
import hudson.security.HudsonPrivateSecurityRealm
import hudson.util.ListBoxModel
import jenkins.model.Jenkins
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class FormUtilIntegrationTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  @Shared
  String usernameFoo = 'foo'

  @Shared
  String usernameBar = 'bar'

  @Shared
  String administrator = 'admin'

  @Shared
  Project fooProject

  def configureJenkins() {
    def securityRealm = new HudsonPrivateSecurityRealm(true, false, null)
    securityRealm.createAccount(usernameFoo, usernameFoo)
    securityRealm.createAccount(usernameBar, usernameBar)
    securityRealm.createAccount(administrator, administrator)
    jenkins.jenkins.setSecurityRealm(securityRealm)

    def authorizationStrategy = new GlobalMatrixAuthorizationStrategy()
    authorizationStrategy.add(Item.CREATE, usernameFoo)
    authorizationStrategy.add(Item.CONFIGURE, usernameFoo)
    Item.CONFIGURE.setEnabled(true)
    authorizationStrategy.add(CredentialsProvider.USE_ITEM, usernameFoo)
    CredentialsProvider.USE_ITEM.setEnabled(true)
    authorizationStrategy.add(Jenkins.READ, usernameFoo)
    authorizationStrategy.add(Jenkins.READ, usernameBar)
    authorizationStrategy.add(Jenkins.ADMINISTER, administrator)
    jenkins.jenkins.setAuthorizationStrategy(authorizationStrategy)

    SystemCredentialsProvider.getInstance().getCredentials().
        add(new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "id", "description", "user", "password"))

    ACL.impersonate(User.get(usernameFoo).impersonate(), {
      fooProject = jenkins.createFreeStyleProject()
      SystemCredentialsProvider.getInstance().getCredentials().
          add(new UsernamePasswordCredentialsImpl(CredentialsScope.USER, "idFoo", "descriptionFoo", "foo", "passwordFoo"))
    })

    jenkins.jenkins.save()
  }

  @Unroll
  def 'displays default credential list with #description'() {
    setup:
      configureJenkins()

    when:
      ListBoxModel credentialList
      ACL.impersonate(User.get(username).impersonate(), {
        credentialList = FormUtil.newCredentialsItemsListBoxModel(serverUrl, credentialsId, ancestor())
      })

    then:
      credentialList.size() == 1
      credentialList[0].name == defaultEntry

    where:
      // Lazy load ancestor as it is null during where clause
      description                                             | username      | serverUrl           | credentialsId | ancestor          | defaultEntry
      'empty credentials, no ancestor, not administrator'     | usernameBar   | 'http://localhost'  | null          | { -> null }       | '- none -'
      'existing credentials, no ancestor, not administrator'  | usernameBar   | 'http://localhost'  | 'somecred'    | { -> null }       | '- current -'
      'empty credentials, ancestor without permission'        | usernameBar   | 'http://localhost'  | null          | { -> fooProject } | '- none -'
      'existing credentials, ancestor without permission'     | usernameBar   | 'http://localhost'  | 'somecred'    | { -> fooProject } | '- current -'
      'empty credentials, no serverUrl'                       | administrator | null                | null          | { -> null }       | '- none -'
      'existing credentials, no serverUrl'                    | administrator | null                | 'somecred'    | { -> null }       | '- current -'
  }

  @Unroll
  def 'displays populated credentials list for #description'() {
    setup:
      configureJenkins()

    when:
      ListBoxModel credentialList
      ACL.impersonate(User.get(username).impersonate(), {
        credentialList = FormUtil.newCredentialsItemsListBoxModel('http://localhost', null, ancestor())
      })

    then:
      credentialList.size() == 3
      credentialList[0].name == '- none -'
      credentialList[1].name == 'foo/****** (descriptionFoo)'
      credentialList[2].name == 'user/****** (description)'

    where:
      // Lazy load ancestor as it is null during where clause
      description   | username       | ancestor
      'project'     | usernameFoo    | { -> fooProject }
      'system'      | administrator  | { -> null }
  }
}
