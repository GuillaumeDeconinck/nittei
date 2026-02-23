package com.meetsmore.nittei.infra.services.google;

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
public class GoogleOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthService.class);
    private static final String TOKEN_REFETCH_ENDPOINT = "https://www.googleapis.com/oauth2/v4/token";
    private static final String CODE_TOKEN_EXCHANGE_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final List<String> REQUIRED_OAUTH_SCOPES = List.of("https://www.googleapis.com/auth/calendar");

    private final WebClient webClient;

    public GoogleOAuthService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public CodeTokenResponse exchangeCodeToken(CodeTokenRequest req) {
        CodeTokenResponse response = webClient.post()
            .uri(CODE_TOKEN_EXCHANGE_ENDPOINT)
            .body(BodyInserters.fromFormData("client_id", req.clientId())
                .with("client_secret", req.clientSecret())
                .with("redirect_uri", req.redirectUri())
                .with("code", req.code())
                .with("grant_type", "authorization_code"))
            .retrieve()
            .bodyToMono(CodeTokenResponse.class)
            .block();

        if (response == null) {
            throw new IllegalStateException("Google OAuth returned empty response");
        }

        List<String> scopes = List.of(response.scope().split(" "));
        for (String requiredScope : REQUIRED_OAUTH_SCOPES) {
            if (!scopes.contains(requiredScope)) {
                throw new IllegalStateException("Missing required Google OAuth scope: " + requiredScope);
            }
        }

        return response;
    }

    public String getAccessToken(User user, NitteiContext context) {
        List<UserIntegration> integrations = context.repos().userIntegrations().find(user.id());
        UserIntegration integration = integrations.stream()
            .filter(i -> i.provider() == IntegrationProvider.GOOGLE)
            .findFirst()
            .orElse(null);

        if (integration == null) {
            return null;
        }

        long now = Instant.now().toEpochMilli();
        if (now + 60_000 <= integration.accessTokenExpiresTs()) {
            return integration.accessToken();
        }

        var googleSettings = context.repos().accountIntegrations().find(user.accountId()).stream()
            .filter(i -> i.provider() == IntegrationProvider.GOOGLE)
            .findFirst()
            .orElse(null);

        if (googleSettings == null) {
            return null;
        }

        Map<String, Object> refreshed = webClient.post()
            .uri(TOKEN_REFETCH_ENDPOINT)
            .body(BodyInserters.fromFormData("client_id", googleSettings.clientId())
                .with("client_secret", googleSettings.clientSecret())
                .with("refresh_token", integration.refreshToken())
                .with("grant_type", "refresh_token"))
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        if (refreshed == null || refreshed.get("access_token") == null || refreshed.get("expires_in") == null) {
            return null;
        }

        String newAccessToken = String.valueOf(refreshed.get("access_token"));
        long expiresInMillis = Long.parseLong(String.valueOf(refreshed.get("expires_in"))) * 1000;

        UserIntegration updated = new UserIntegration(
            integration.userId(),
            integration.accountId(),
            integration.provider(),
            integration.refreshToken(),
            newAccessToken,
            now + expiresInMillis
        );

        try {
            context.repos().userIntegrations().save(updated);
        } catch (RuntimeException ex) {
            log.error("Unable to save updated google credentials for user {}", user.id().value(), ex);
        }

        return newAccessToken;
    }
}
