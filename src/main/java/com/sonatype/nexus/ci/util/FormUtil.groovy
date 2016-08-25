/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.util

import com.cloudbees.plugins.credentials.common.StandardListBoxModel
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import hudson.security.ACL
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import jenkins.model.Jenkins

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials
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
      return FormValidation.error("Malformed url (%s)", e.getMessage())
    }
  }

  static FormValidation validateNotEmpty(String value, String error) {
    if (!value) {
      return FormValidation.error(error)
    }
    return FormValidation.ok()
  }

  static ListBoxModel buildCredentialsItems(final String serverUrl) {
    if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
      return new ListBoxModel()
    }
    return new StandardListBoxModel()
        .withEmptySelection()
        .withAll(lookupCredentials(
        StandardUsernamePasswordCredentials.class,
        Jenkins.getInstance(),
        ACL.SYSTEM, fromUri(serverUrl).build())
    )
  }

  static ListBoxModel buildListBoxModel(Closure<String> nameValueSelector, List items) {
    return buildListBoxModel({ nameValueSelector(it) }, { nameValueSelector(it) }, items)
  }

  static ListBoxModel buildListBoxModel(Closure<String> nameSelector, Closure<String> valueSelector, List items) {
    def listBoxModel = buildListBoxModelWithEmptyOption()
    items.each { item ->
      listBoxModel.add(nameSelector(item), valueSelector(item))
    }
    return listBoxModel
  }

  static ListBoxModel buildListBoxModelWithEmptyOption() {
    def listBoxModel = new ListBoxModel()
    listBoxModel.add(EMPTY_LIST_BOX_NAME, EMPTY_LIST_BOX_VALUE)
    return listBoxModel
  }
}
