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
package org.sonatype.nexus.ci.iq

import java.util.logging.Level

import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.jvnet.hudson.test.LoggerRule
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Add this trait to any test that should have an extra verification that no JEP-200 marshalling errors have occurred.
 * See INT-2407 and https://docs.sonatype.com/x/op5NCQ for additional information and INT-418 for the original work.
 */
trait ClassFilterLoggingTestTrait
{
  Logger logger = LoggerFactory.getLogger(ClassFilterLoggingTestTrait)

  @Rule
  LoggerRule loggerRule = new LoggerRule()

  def helpfulMessage = '''A possible JEP-200 marshalling error has occurred. It is likely that additional classes 
will need to be added to the 'hudson.remoting.ClassFilter' file. See https://docs.sonatype.com/x/op5NCQ'''

  @Before
  def setupLoggingTest() {
    loggerRule.capture(10).record(CpsFlowExecution, Level.WARNING)
  }

  @After
  def runLoggingTest() {
    loggerRule.records.findAll { it.thrown }.each { record -> assertNoClassFilterErrors(record.thrown) }
  }

  /**
   * Recursively look at the causes for specific keywords. Note: These might change over time which would render this
   * test moot. Hence we are checking for a few keywords including the specific redirect URL.
   */
  def assertNoClassFilterErrors(Throwable throwable) {
    def msg = throwable.message
    assert !msg.contains("Failed to serialize"), helpfulMessage
    assert !msg.contains("Refusing to marshal"), helpfulMessage
    assert !msg.contains("https://jenkins.io/redirect/class-filter/"), helpfulMessage
    if (throwable.cause != null) {
      assertNoClassFilterErrors(throwable.cause)
    }
  }
}
