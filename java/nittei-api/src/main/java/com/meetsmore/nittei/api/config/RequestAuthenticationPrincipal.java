package com.meetsmore.nittei.api.config;

public record RequestAuthenticationPrincipal(
    AuthenticationScheme scheme, String credential, String accountIdHeader) {

  public enum AuthenticationScheme {
    API_KEY,
    BEARER_TOKEN,
    PUBLIC_ACCOUNT
  }

  public static RequestAuthenticationPrincipal admin(String apiKey) {
    return new RequestAuthenticationPrincipal(AuthenticationScheme.API_KEY, apiKey, null);
  }

  public static RequestAuthenticationPrincipal user(String bearerToken, String accountIdHeader) {
    return new RequestAuthenticationPrincipal(
        AuthenticationScheme.BEARER_TOKEN, bearerToken, accountIdHeader);
  }

  public static RequestAuthenticationPrincipal publicAccount(String accountIdHeader) {
    return new RequestAuthenticationPrincipal(
        AuthenticationScheme.PUBLIC_ACCOUNT, accountIdHeader, accountIdHeader);
  }
}
