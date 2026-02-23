package com.meetsmore.nittei.infra.services;

public record CodeTokenResponse(String accessToken, String scope, String tokenType, long expiresIn, String refreshToken) {
}
