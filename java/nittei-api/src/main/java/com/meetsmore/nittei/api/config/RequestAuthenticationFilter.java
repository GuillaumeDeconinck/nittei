package com.meetsmore.nittei.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestAuthenticationFilter extends OncePerRequestFilter {

  static final String API_KEY_HEADER = "x-api-key";
  static final String ACCOUNT_HEADER = "nittei-account";
  private static final String BEARER_PREFIX = "bearer ";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      UsernamePasswordAuthenticationToken authentication = resolveAuthentication(request);
      if (authentication != null) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }

    filterChain.doFilter(request, response);
  }

  private UsernamePasswordAuthenticationToken resolveAuthentication(HttpServletRequest request) {
    String apiKey = request.getHeader(API_KEY_HEADER);
    if (apiKey != null && !apiKey.isBlank()) {
      return new UsernamePasswordAuthenticationToken(
          RequestAuthenticationPrincipal.admin(apiKey),
          null,
          List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (isBearerAuthorization(authorization)) {
      String accountId = request.getHeader(ACCOUNT_HEADER);
      return new UsernamePasswordAuthenticationToken(
          RequestAuthenticationPrincipal.user(
              extractBearerToken(authorization), blankToNull(accountId)),
          null,
          List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    String accountId = request.getHeader(ACCOUNT_HEADER);
    if (isAccountHeader(accountId)) {
      return new UsernamePasswordAuthenticationToken(
          RequestAuthenticationPrincipal.publicAccount(accountId.trim()),
          null,
          List.of(new SimpleGrantedAuthority("ROLE_PUBLIC_ACCOUNT")));
    }

    return null;
  }

  static boolean isBearerAuthorization(String authorization) {
    if (authorization == null || authorization.isBlank()) {
      return false;
    }
    if (!authorization.toLowerCase(Locale.ROOT).startsWith(BEARER_PREFIX)) {
      return false;
    }
    return !authorization.substring(BEARER_PREFIX.length()).trim().isBlank();
  }

  static boolean isAccountHeader(String accountId) {
    if (accountId == null || accountId.isBlank()) {
      return false;
    }
    try {
      UUID.fromString(accountId.trim());
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  static String extractBearerToken(String authorization) {
    return authorization.substring(BEARER_PREFIX.length()).trim();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
