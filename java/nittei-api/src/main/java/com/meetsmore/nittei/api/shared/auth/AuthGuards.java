package com.meetsmore.nittei.api.shared.auth;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ResponseStatusException;

public final class AuthGuards {

  public static final String NITTEI_X_API_KEY_HEADER = "x-api-key";

  private AuthGuards() {}

  public static void requireAdmin(HttpHeaders headers) {
    String apiKey = headers.getFirst(NITTEI_X_API_KEY_HEADER);
    if (apiKey == null || apiKey.isBlank()) {
      throw new ResponseStatusException(UNAUTHORIZED, "Missing x-api-key header");
    }
  }

  public static void requireUser(HttpHeaders headers) {
    String auth = headers.getFirst(HttpHeaders.AUTHORIZATION);
    if (auth == null || auth.isBlank() || !auth.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
      throw new ResponseStatusException(UNAUTHORIZED, "Missing or invalid Authorization header");
    }
  }

  public static void requirePublicAccount(HttpHeaders headers) {
    String accountId = headers.getFirst("nittei-account");
    if (accountId == null || accountId.isBlank()) {
      requireAdmin(headers);
    }
  }
}
