package com.meetsmore.nittei.infra.services;

public record CodeTokenRequest(String clientId, String clientSecret, String redirectUri, String code) {
}
