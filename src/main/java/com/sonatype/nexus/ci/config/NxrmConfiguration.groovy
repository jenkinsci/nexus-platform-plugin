/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.config

import hudson.model.Describable
import hudson.model.Descriptor
import jenkins.model.Jenkins

abstract class NxrmConfiguration
    implements Describable<NxrmConfiguration>
{
  String id

  /**
   * Used as a unique identifier per instance to ensure unique Display Name and Id
   */
  String internalId

  String displayName

  String serverUrl

  String credentialsId

  @Override
  Descriptor<NxrmConfiguration> getDescriptor() {
    return Jenkins.getInstance().getDescriptorOrDie(this.getClass());
  }

  /**
   * Currently NxrmConfigurations are selected from all extended {@link hudson.model.Descriptor} so generic type should
   * be {@link com.sonatype.nexus.ci.config.NxrmConfiguration} in order to build the view's
   * {@link lib.FormTagLib#repeatableHeteroProperty(java.util.Map, groovy.lang.Closure)}
   */
  static abstract class NxrmDescriptor
      extends Descriptor<NxrmConfiguration>
  {
    public NxrmDescriptor(Class<? extends NxrmConfiguration> clazz) {
      super(clazz)
    }
  }
}
