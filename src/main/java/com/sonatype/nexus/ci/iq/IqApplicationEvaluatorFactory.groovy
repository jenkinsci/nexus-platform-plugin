/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import com.sonatype.nexus.api.iq.IqClient

import org.codehaus.plexus.util.DirectoryScanner

class IqApplicationEvaluatorFactory
{
  static IqApplicationEvaluator getPolicyEvaluator(final IqClient iqClient) {
    new IqApplicationEvaluator(iqClient, new DirectoryScanner())
  }
}
