package com.meetsmore.nittei.infra.services.outlook;

import com.meetsmore.nittei.domain.IntegrationProvider;
import com.meetsmore.nittei.domain.User;
import com.meetsmore.nittei.domain.UserIntegration;
import com.meetsmore.nittei.infra.context.NitteiContext;
import com.meetsmore.nittei.infra.services.CodeTokenRequest;
import com.meetsmore.nittei.infra.services.CodeTokenResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class OutlookOAuthService {

  private static final Logger log = LoggerFactory.getLogger(OutlookOAuthService.class);
  private static final String TOKEN_REFETCH_ENDPOINT =
      "https://login.microsoftonline.com/common/oauth2/v2.0/token";
  private static final String CODE_TOKEN_EXCHANGE_ENDPOINT =
      "https://login.microsoftonline.com/common/oauth2/v2.0/token";
  private static final List<String> REQUIRED_OAUTH_SCOPES =
      List.of("https://graph.microsoft.com/calendars.readwrite", "offline_access");

  private final WebClient webClient;

  public OutlookOAuthService(WebClient.Builder webClientBuilder) {
    this.webClient = webClientBuilder.build();
  }

  public CodeTokenResponse exchangeCodeToken(CodeTokenRequest req) {
    CodeTokenResponse response =
        webClient
            .post()
            .uri(CODE_TOKEN_EXCHANGE_ENDPOINT)
            .body(
                BodyInserters.fromFormData("client_id", req.clientId())
                    .with("client_secret", req.clientSecret())
                    .with("redirect_uri", req.redirectUri())
                    .with("code", req.code())
                    .with("scope", String.join(" ", REQUIRED_OAUTH_SCOPES))
                    .with("grant_type", "authorization_code"))
            .retrieve()
            .bodyToMono(CodeTokenResponse.class)
            .block();

    if (response == null) {
      throw new IllegalStateException("Outlook OAuth returned empty response");
    }

    List<String> scopes = List.of(response.scope().toLowerCase().split(" "));
    if (!scopes.contains("https://graph.microsoft.com/calendars.readwrite")) {
      throw new IllegalStateException("Missing required Outlook OAuth scope");
    }

    return response;
  }

  public String getAccessToken(User user, NitteiContext context) {
    List<UserIntegration> integrations = context.repos().userIntegrations().find(user.id());
    UserIntegration integration =
        integrations.stream()
            .filter(i -> i.provider() == IntegrationProvider.OUTLOOK)
            .findFirst()
            .orElse(null);

    if (integration == null) {
      return null;
    }

    long now = Instant.now().toEpochMilli();
    if (now + 60_000 <= integration.accessTokenExpiresTs()) {
      return integration.accessToken();
    }

    var outlookSettings =
        context.repos().accountIntegrations().find(user.accountId()).stream()
            .filter(i -> i.provider() == IntegrationProvider.OUTLOOK)
            .findFirst()
            .orElse(null);

    if (outlookSettings == null) {
      return null;
    }

    Map<String, Object> refreshed =
        webClient
            .post()
            .uri(TOKEN_REFETCH_ENDPOINT)
            .body(
                BodyInserters.fromFormData("client_id", outlookSettings.clientId())
                    .with("client_secret", outlookSettings.clientSecret())
                    .with("redirect_uri", outlookSettings.redirectUri())
                    .with("refresh_token", integration.refreshToken())
                    .with("scope", String.join(" ", REQUIRED_OAUTH_SCOPES))
                    .with("grant_type", "refresh_token"))
            .retrieve()
            .bodyToMono(Map.class)
            .block();

    if (refreshed == null
        || refreshed.get("access_token") == null
        || refreshed.get("expires_in") == null) {
      return null;
    }

    String newAccessToken = String.valueOf(refreshed.get("access_token"));
    long expiresInMillis = Long.parseLong(String.valueOf(refreshed.get("expires_in"))) * 1000;

    UserIntegration updated =
        new UserIntegration(
            integration.userId(),
            integration.accountId(),
            integration.provider(),
            integration.refreshToken(),
            newAccessToken,
            now + expiresInMillis);

    try {
      context.repos().userIntegrations().save(updated);
    } catch (RuntimeException ex) {
      log.error("Unable to save updated outlook credentials for user {}", user.id().value(), ex);
    }

    return newAccessToken;
  }
}
