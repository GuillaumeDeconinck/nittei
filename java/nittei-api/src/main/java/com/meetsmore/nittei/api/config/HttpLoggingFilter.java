package com.meetsmore.nittei.api.config;

import com.meetsmore.nittei.utils.config.AppConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HttpLoggingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(HttpLoggingFilter.class);
  private static final Set<String> EXCLUDED_PATHS =
      Set.of("/api/v1/healthcheck", "/api/v1/metrics");
  public static final String REQUEST_ID_HEADER = "X-Request-Id";
  public static final String REQUEST_ID_MDC_KEY = "request_id";

  private final AppConfig appConfig;

  public HttpLoggingFilter(AppConfig appConfig) {
    this.appConfig = appConfig;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String requestId = resolveRequestId(request);
    long startNanos = System.nanoTime();
    request.setAttribute(REQUEST_ID_MDC_KEY, requestId);
    response.setHeader(REQUEST_ID_HEADER, requestId);

    try {
      MDC.put(REQUEST_ID_MDC_KEY, requestId);
      chain.doFilter(request, response);
    } finally {
      MDC.remove(REQUEST_ID_MDC_KEY);
    }

    String path = request.getRequestURI();
    int status = response.getStatus();

    if (!appConfig.getObservability().isObserveStatusEndpoints()
        && EXCLUDED_PATHS.contains(path)
        && status == HttpServletResponse.SC_OK) {
      return;
    }

    long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
    log.info("{} {} {} {}ms", request.getMethod(), request.getRequestURI(), status, elapsedMs);
  }

  private String resolveRequestId(HttpServletRequest request) {
    String requestId = request.getHeader(REQUEST_ID_HEADER);
    if (requestId == null || requestId.isBlank()) {
      return UUID.randomUUID().toString();
    }
    return requestId;
  }
}
