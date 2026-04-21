package com.meetsmore.nittei.utils.backtrace;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class BacktraceUtils {

  private BacktraceUtils() {}

  public static String appFocusedBacktrace(Throwable throwable) {
    return Arrays.stream(throwable.getStackTrace())
        .map(StackTraceElement::toString)
        .filter(
            line ->
                (line.contains("nittei")
                        || line.contains("com.meetsmore")
                        || line.contains("crates")
                        || line.contains("bins.nittei"))
                    && !line.contains("java.base")
                    && !line.contains("org.springframework"))
        .collect(Collectors.joining("\n"));
  }
}
