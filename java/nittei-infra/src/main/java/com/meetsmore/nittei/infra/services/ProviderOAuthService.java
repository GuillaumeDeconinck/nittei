package com.meetsmore.nittei.infra.services;

import com.meetsmore.nittei.domain.IntegrationProvider;
import com.meetsmore.nittei.infra.services.google.GoogleOAuthService;
import com.meetsmore.nittei.infra.services.outlook.OutlookOAuthService;
import org.springframework.stereotype.Service;

@Service
public class ProviderOAuthService implements ProviderOAuth {

  private final GoogleOAuthService googleOAuthService;
  private final OutlookOAuthService outlookOAuthService;

  public ProviderOAuthService(
      GoogleOAuthService googleOAuthService, OutlookOAuthService outlookOAuthService) {
    this.googleOAuthService = googleOAuthService;
    this.outlookOAuthService = outlookOAuthService;
  }

  public CodeTokenResponse exchangeCodeToken(
      IntegrationProvider provider, CodeTokenRequest request) {
    return switch (provider) {
      case GOOGLE -> googleOAuthService.exchangeCodeToken(request);
      case OUTLOOK -> outlookOAuthService.exchangeCodeToken(request);
    };
  }

  @Override
  public CodeTokenResponse exchangeCodeToken(CodeTokenRequest request) {
    throw new UnsupportedOperationException("Use exchangeCodeToken(provider, request)");
  }
}
