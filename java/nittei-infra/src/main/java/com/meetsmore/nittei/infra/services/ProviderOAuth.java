package com.meetsmore.nittei.infra.services;

public interface ProviderOAuth {
  CodeTokenResponse exchangeCodeToken(CodeTokenRequest request);
}
