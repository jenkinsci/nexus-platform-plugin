package org.sonatype.nexus.ci.iq

import java.util.logging.Level
import java.util.logging.LogRecord

import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.jvnet.hudson.test.LoggerRule
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Add this trait to any test that should have an extra verification that no JEP-200 marshalling errors have occurred.
 * See INT-2407 for additional information and INT-418 for original work.
 */
trait ClassFilterLoggingTestTrait
{
  Logger logger = LoggerFactory.getLogger(ClassFilterLoggingTestTrait)

  @Rule
  public LoggerRule loggerRule = new LoggerRule()

  def helpfulMessage = '''A possible JEP-200 marshalling error has occurred. It is likely that additional classes 
will need to be added to the 'hudson.remoting.ClassFilter' file. See INT-2407 for additional info'''

  @Before
  def setupLoggingTest() {
    loggerRule.capture(10).record(CpsFlowExecution, Level.WARNING)
  }

  @After
  def runLoggingTest() {
    for (LogRecord logRecord : loggerRule.records) {
      if (logRecord.thrown != null) {
        assertNoClassFilterErrors(logRecord.thrown)
      }
    }
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
