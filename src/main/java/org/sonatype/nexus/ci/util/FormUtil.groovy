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

import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials
import com.cloudbees.plugins.credentials.common.StandardCredentials
import com.cloudbees.plugins.credentials.common.StandardListBoxModel
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import hudson.model.Item
import hudson.model.ItemGroup
import hudson.security.ACL
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import jenkins.model.Jenkins

import static com.cloudbees.plugins.credentials.CredentialsMatchers.anyOf
import static com.cloudbees.plugins.credentials.CredentialsMatchers.instanceOf
import static com.cloudbees.plugins.credentials.domains.URIRequirementBuilder.fromUri

class FormUtil
{
  final static String EMPTY_LIST_BOX_NAME = '-----------'

  final static String EMPTY_LIST_BOX_VALUE = ''

  static FormValidation validateUrl(String url) {
    try {
      if (url) {
        new URL(url)
      }

      return FormValidation.ok()
    }
    catch (MalformedURLException e) {
      return FormValidation.error('Malformed url (%s)', e.getMessage())
    }
  }

  static FormValidation validateNotEmpty(String value, String error) {
    if (!value) {
      return FormValidation.error(error)
    }
    return FormValidation.ok()
  }

  static FormValidation validateNoWhitespace(String value, String error) {
    if (value ==~ /.*\s+?.*/) {
      return FormValidation.error(error)
    }
    return FormValidation.ok()
  }

  static ListBoxModel newCredentialsItemsListBoxModel(final String serverUrl,
                                                      final String credentialsId,
                                                      final Item context) {
    return getCredentialsItemsListBoxModel(serverUrl, credentialsId, context)
  }

  static ListBoxModel newCredentialsItemsListBoxModel(final String serverUrl,
                                                      final String credentialsId,
                                                      final ItemGroup context) {
    return getCredentialsItemsListBoxModel(serverUrl, credentialsId, context)
  }

  private static ListBoxModel getCredentialsItemsListBoxModel(final String serverUrl,
                                                              final String credentialsId,
                                                              final context)
  {
    if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER) || !serverUrl) {
      return new StandardListBoxModel().includeCurrentValue(credentialsId)
    }
    //noinspection GroovyAssignabilityCheck
    return new StandardListBoxModel()
        .includeEmptyValue()
        .includeMatchingAs(ACL.SYSTEM,
          context,
          StandardCredentials,
          fromUri(serverUrl).build(),
          anyOf(instanceOf(StandardUsernamePasswordCredentials), instanceOf(StandardCertificateCredentials)))

  }

  static ListBoxModel newListBoxModel(Closure<String> nameSelector, Closure<String> valueSelector, List items)
  {
    def listBoxModel = newListBoxModelWithEmptyOption()
    items.each { item ->
      listBoxModel.add(nameSelector(item), valueSelector(item))
    }
    return listBoxModel
  }

  static ListBoxModel newListBoxModelWithEmptyOption() {
    def listBoxModel = new ListBoxModel()
    listBoxModel.add(EMPTY_LIST_BOX_NAME, EMPTY_LIST_BOX_VALUE)
    return listBoxModel
  }
}
