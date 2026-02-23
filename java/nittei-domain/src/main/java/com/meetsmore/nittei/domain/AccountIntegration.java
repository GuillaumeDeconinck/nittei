package com.meetsmore.nittei.domain;

public record AccountIntegration(
    ID accountId,
    String clientId,
    String clientSecret,
    String redirectUri,
    IntegrationProvider provider
) {
}
