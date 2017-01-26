/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.util

import hudson.model.TaskListener;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

final class LoggerBridge
    extends MarkerIgnoringBase
{
  private static final boolean DEBUG = Boolean.getBoolean(LoggerBridge.class.getName() + ".debug");

  private final TaskListener listener;

  public LoggerBridge(final TaskListener listener) {
    this.listener = listener;
  }

  @Override
  public String getName() {
    return "LoggerBridge";
  }

  @Override
  protected Object readResolve() {
    return this; // restore original behaviour
  }

  @Override
  public boolean isTraceEnabled() {
    return DEBUG;
  }

  @Override
  public boolean isDebugEnabled() {
    return DEBUG;
  }

  @Override
  public boolean isInfoEnabled() {
    return true;
  }

  @Override
  public boolean isWarnEnabled() {
    return true;
  }

  @Override
  public boolean isErrorEnabled() {
    return true;
  }

  @Override
  public void trace(final String msg) {
    if (DEBUG) {
      log("[TRACE]", msg, null);
    }
  }

  @Override
  public void trace(final String format, final Object arg) {
    if (DEBUG) {
      log("[TRACE]", MessageFormatter.format(format, arg));
    }
  }

  @Override
  public void trace(final String format, final Object arg1, final Object arg2) {
    if (DEBUG) {
      log("[TRACE]", MessageFormatter.format(format, arg1, arg2));
    }
  }

  @Override
  public void trace(final String format, final Object[] argArray) {
    if (DEBUG) {
      log("[TRACE]", MessageFormatter.arrayFormat(format, argArray));
    }
  }

  @Override
  public void trace(final String msg, final Throwable t) {
    if (DEBUG) {
      log("[TRACE]", msg, t);
    }
  }

  @Override
  public void debug(final String msg) {
    if (DEBUG) {
      log("[DEBUG]", msg, null);
    }
  }

  @Override
  public void debug(final String format, final Object arg) {
    if (DEBUG) {
      log("[DEBUG]", MessageFormatter.format(format, arg));
    }
  }

  @Override
  public void debug(final String format, final Object arg1, final Object arg2) {
    if (DEBUG) {
      log("[DEBUG]", MessageFormatter.format(format, arg1, arg2));
    }
  }

  @Override
  public void debug(final String format, final Object[] argArray) {
    if (DEBUG) {
      log("[DEBUG]", MessageFormatter.arrayFormat(format, argArray));
    }
  }

  @Override
  public void debug(final String msg, final Throwable t) {
    if (DEBUG) {
      log("[DEBUG]", msg, t);
    }
  }

  @Override
  public void info(final String msg) {
    log("[INFO]", msg, null);
  }

  @Override
  public void info(final String format, final Object arg) {
    log("[INFO]", MessageFormatter.format(format, arg));
  }

  @Override
  public void info(final String format, final Object arg1, final Object arg2) {
    log("[INFO]", MessageFormatter.format(format, arg1, arg2));
  }

  @Override
  public void info(final String format, final Object[] argArray) {
    log("[INFO]", MessageFormatter.arrayFormat(format, argArray));
  }

  @Override
  public void info(final String msg, final Throwable t) {
    log("[INFO]", msg, t);
  }

  @Override
  public void warn(final String msg) {
    log("[WARN]", msg, null);
  }

  @Override
  public void warn(final String format, final Object arg) {
    log("[WARN]", MessageFormatter.format(format, arg));
  }

  @Override
  public void warn(final String format, final Object arg1, final Object arg2) {
    log("[WARN]", MessageFormatter.format(format, arg1, arg2));
  }

  @Override
  public void warn(final String format, final Object[] argArray) {
    log("[WARN]", MessageFormatter.arrayFormat(format, argArray));
  }

  @Override
  public void warn(final String msg, final Throwable t) {
    log("[WARN]", msg, t);
  }

  @Override
  public void error(final String msg) {
    log("[ERROR]", msg, null);
  }

  @Override
  public void error(final String format, final Object arg) {
    log("[ERROR]", MessageFormatter.format(format, arg));
  }

  @Override
  public void error(final String format, final Object arg1, final Object arg2) {
    log("[ERROR]", MessageFormatter.format(format, arg1, arg2));
  }

  @Override
  public void error(final String format, final Object[] argArray) {
    log("[ERROR]", MessageFormatter.arrayFormat(format, argArray));
  }

  @Override
  public void error(final String msg, final Throwable t) {
    log("[ERROR]", msg, t);
  }

  private void log(final String tag, final FormattingTuple format) {
    log(tag, format.getMessage(), format.getThrowable());
  }

  private void log(final String tag, final String msg, final Throwable t) {
    listener.getLogger().println(tag + " " + msg);
    if (t != null) {
      t.printStackTrace(listener.getLogger());
    }
  }
}
