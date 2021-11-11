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

import hudson.model.TaskListener
import org.slf4j.helpers.FormattingTuple
import org.slf4j.helpers.MarkerIgnoringBase
import org.slf4j.helpers.MessageFormatter

// method count is high because of overriding base class
@SuppressWarnings(value = ['MethodCount', 'ConfusingMethodName', 'ClassSize'])
final class LoggerBridge
    extends MarkerIgnoringBase
{
  private boolean debugEnabled = Boolean.valueOf(System.getProperty(LoggerBridge.getName() + '.debug'))

  private static final String TRACE_PREFIX = '[TRACE]'

  private static final String DEBUG_PREFIX = '[DEBUG]'

  private static final String INFO_PREFIX = '[INFO]'

  private static final String WARN_PREFIX = '[WARN]'

  private static final String ERROR_PREFIX = '[ERROR]'

  private final TaskListener listener

  LoggerBridge(final TaskListener listener) {
    this.listener = listener
  }

  @Override
   String getName() {
    return 'LoggerBridge'
  }

  @Override
  protected Object readResolve() {
    return this // restore original behaviour
  }

  @Override
   boolean isTraceEnabled() {
    return debugEnabled
  }

  @Override
   boolean isDebugEnabled() {
    return debugEnabled
  }

  void setDebugEnabled(boolean debugEnabled) {
    this.debugEnabled = debugEnabled
  }

  @Override
   boolean isInfoEnabled() {
    return true
  }

  @Override
   boolean isWarnEnabled() {
    return true
  }

  @Override
   boolean isErrorEnabled() {
    return true
  }

  @Override
   void trace(final String msg) {
    if (debugEnabled) {
      log(TRACE_PREFIX, msg, null)
    }
  }

  @Override
   void trace(final String format, final Object arg) {
    if (debugEnabled) {
      log(TRACE_PREFIX, MessageFormatter.format(format, arg))
    }
  }

  @Override
   void trace(final String format, final Object arg1, final Object arg2) {
    if (debugEnabled) {
      log(TRACE_PREFIX, MessageFormatter.format(format, arg1, arg2))
    }
  }

  @Override
   void trace(final String format, final Object[] argArray) {
    if (debugEnabled) {
      log(TRACE_PREFIX, MessageFormatter.arrayFormat(format, argArray))
    }
  }

  @Override
   void trace(final String msg, final Throwable t) {
    if (debugEnabled) {
      log(TRACE_PREFIX, msg, t)
    }
  }

  @Override
   void debug(final String msg) {
    if (debugEnabled) {
      log(DEBUG_PREFIX, msg, null)
    }
  }

  @Override
   void debug(final String format, final Object arg) {
    if (debugEnabled) {
      log(DEBUG_PREFIX, MessageFormatter.format(format, arg))
    }
  }

  @Override
   void debug(final String format, final Object arg1, final Object arg2) {
    if (debugEnabled) {
      log(DEBUG_PREFIX, MessageFormatter.format(format, arg1, arg2))
    }
  }

  @Override
   void debug(final String format, final Object[] argArray) {
    if (debugEnabled) {
      log(DEBUG_PREFIX, MessageFormatter.arrayFormat(format, argArray))
    }
  }

  @Override
   void debug(final String msg, final Throwable t) {
    if (debugEnabled) {
      log(DEBUG_PREFIX, msg, t)
    }
  }

  @Override
   void info(final String msg) {
    log(INFO_PREFIX, msg, null)
  }

  @Override
   void info(final String format, final Object arg) {
    log(INFO_PREFIX, MessageFormatter.format(format, arg))
  }

  @Override
   void info(final String format, final Object arg1, final Object arg2) {
    log(INFO_PREFIX, MessageFormatter.format(format, arg1, arg2))
  }

  @Override
   void info(final String format, final Object[] argArray) {
    log(INFO_PREFIX, MessageFormatter.arrayFormat(format, argArray))
  }

  @Override
   void info(final String msg, final Throwable t) {
    log(INFO_PREFIX, msg, t)
  }

  @Override
   void warn(final String msg) {
    log(WARN_PREFIX, msg, null)
  }

  @Override
   void warn(final String format, final Object arg) {
    log(WARN_PREFIX, MessageFormatter.format(format, arg))
  }

  @Override
   void warn(final String format, final Object arg1, final Object arg2) {
    log(WARN_PREFIX, MessageFormatter.format(format, arg1, arg2))
  }

  @Override
   void warn(final String format, final Object[] argArray) {
    log(WARN_PREFIX, MessageFormatter.arrayFormat(format, argArray))
  }

  @Override
   void warn(final String msg, final Throwable t) {
    log(WARN_PREFIX, msg, t)
  }

  @Override
   void error(final String msg) {
    log(ERROR_PREFIX, msg, null)
  }

  @Override
   void error(final String format, final Object arg) {
    log(ERROR_PREFIX, MessageFormatter.format(format, arg))
  }

  @Override
   void error(final String format, final Object arg1, final Object arg2) {
    log(ERROR_PREFIX, MessageFormatter.format(format, arg1, arg2))
  }

  @Override
   void error(final String format, final Object[] argArray) {
    log(ERROR_PREFIX, MessageFormatter.arrayFormat(format, argArray))
  }

  @Override
   void error(final String msg, final Throwable t) {
    log(ERROR_PREFIX, msg, t)
  }

  private void log(final String tag, final FormattingTuple format) {
    log(tag, format.getMessage(), format.getThrowable())
  }

  private void log(final String tag, final String msg, final Throwable t) {
    listener.getLogger().println(tag + ' ' + msg)
    if (t != null) {
      t.printStackTrace(listener.getLogger())
    }
  }
}
