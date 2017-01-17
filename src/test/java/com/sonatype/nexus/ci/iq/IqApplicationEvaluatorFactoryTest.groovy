/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import com.sonatype.nexus.api.iq.IqClient

import spock.lang.Specification

class IqApplicationEvaluatorFactoryTest
    extends Specification
{
  def 'factory creates new instance of application evaluator'() {
    setup:
      def iqClient = Mock(IqClient)

    when:
      def iqApplicationEvaluator = IqApplicationEvaluatorFactory.getPolicyEvaluator(iqClient)

    then:
      assert iqApplicationEvaluator.directoryScanner != null
      assert iqApplicationEvaluator.iqClient == iqClient
  }
}
