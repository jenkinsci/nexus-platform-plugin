/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import spock.lang.Specification

class IqPolicyEvaluationExceptionTest
    extends Specification
{
  def 'creates exception with message'() {
    expect:
      new IqPolicyEvaluationException("message").getMessage() == "message"
  }
}
