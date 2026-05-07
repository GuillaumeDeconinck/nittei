package com.meetsmore.nittei.utils.backtrace;

import org.slf4j.Logger;

public final class BacktraceLogger {

  private BacktraceLogger() {}

  public static void errorWithBacktrace(Logger log, Throwable throwable, String message) {
    String filtered = BacktraceUtils.appFocusedBacktrace(throwable);
    log.error("{}\nbacktrace={} ", message, filtered, throwable);
  }
}
