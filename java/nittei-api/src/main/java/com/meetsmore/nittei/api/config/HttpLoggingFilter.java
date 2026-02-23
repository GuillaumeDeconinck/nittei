package com.meetsmore.nittei.api.config;

import com.meetsmore.nittei.utils.config.AppConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HttpLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpLoggingFilter.class);
    private static final Set<String> EXCLUDED_PATHS = Set.of("/api/v1/healthcheck", "/api/v1/metrics");

    private final AppConfig appConfig;

    public HttpLoggingFilter(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        long startNanos = System.nanoTime();
        chain.doFilter(request, response);

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
}
