package com.meetsmore.nittei.api.error;

import java.time.Instant;

public record ApiErrorResponse(
    Instant timestamp, int status, String code, String message, String path, String requestId) {
  public static ApiErrorResponse of(
      int status, String code, String message, String path, String requestId) {
    return new ApiErrorResponse(Instant.now(), status, code, message, path, requestId);
  }
}
