package com.meetsmore.nittei.domain;

public record UserIntegration(
    ID userId,
    ID accountId,
    IntegrationProvider provider,
    String refreshToken,
    String accessToken,
    long accessTokenExpiresTs) {}
