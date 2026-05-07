package com.meetsmore.nittei.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetsmore.nittei.api.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {
  private final ObjectMapper objectMapper;

  public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authenticationException)
      throws IOException {
    writeError(
        response,
        HttpStatus.UNAUTHORIZED,
        "UNAUTHORIZED",
        "Authentication is required to access this resource",
        request);
  }

  static void writeError(
      HttpServletResponse response,
      HttpStatus status,
      String code,
      String message,
      HttpServletRequest request,
      ObjectMapper objectMapper)
      throws IOException {
    String requestId = ensureRequestId(request, response);
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getOutputStream(),
        ApiErrorResponse.of(status.value(), code, message, request.getRequestURI(), requestId));
  }

  private void writeError(
      HttpServletResponse response,
      HttpStatus status,
      String code,
      String message,
      HttpServletRequest request)
      throws IOException {
    writeError(response, status, code, message, request, objectMapper);
  }

  private static String ensureRequestId(HttpServletRequest request, HttpServletResponse response) {
    Object requestId = request.getAttribute(HttpLoggingFilter.REQUEST_ID_MDC_KEY);
    if (requestId != null) {
      return String.valueOf(requestId);
    }

    String resolvedRequestId = UUID.randomUUID().toString();
    request.setAttribute(HttpLoggingFilter.REQUEST_ID_MDC_KEY, resolvedRequestId);
    response.setHeader(HttpLoggingFilter.REQUEST_ID_HEADER, resolvedRequestId);
    return resolvedRequestId;
  }
}
