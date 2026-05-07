package com.meetsmore.nittei.infra.services;

import com.meetsmore.nittei.domain.User;
import com.meetsmore.nittei.domain.providers.google.GoogleCalendarAccessRole;
import com.meetsmore.nittei.domain.providers.google.GoogleCalendarListEntry;
import com.meetsmore.nittei.domain.providers.outlook.OutlookCalendar;
import com.meetsmore.nittei.domain.providers.outlook.OutlookCalendarAccessRole;
import com.meetsmore.nittei.infra.context.NitteiContext;
import com.meetsmore.nittei.infra.services.google.GoogleCalendarRestApi;
import com.meetsmore.nittei.infra.services.google.GoogleOAuthService;
import com.meetsmore.nittei.infra.services.outlook.OutlookCalendarRestApi;
import com.meetsmore.nittei.infra.services.outlook.OutlookOAuthService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ProviderCalendarService {

  private final GoogleOAuthService googleOAuthService;
  private final OutlookOAuthService outlookOAuthService;
  private final WebClient.Builder webClientBuilder;

  public ProviderCalendarService(
      GoogleOAuthService googleOAuthService,
      OutlookOAuthService outlookOAuthService,
      WebClient.Builder webClientBuilder) {
    this.googleOAuthService = googleOAuthService;
    this.outlookOAuthService = outlookOAuthService;
    this.webClientBuilder = webClientBuilder;
  }

  public List<GoogleCalendarListEntry> listGoogleCalendars(
      User user, GoogleCalendarAccessRole minAccessRole, NitteiContext ctx) {
    String token = googleOAuthService.getAccessToken(user, ctx);
    if (token == null || token.isBlank()) {
      return null;
    }
    GoogleCalendarAccessRole accessRole =
        minAccessRole == null ? GoogleCalendarAccessRole.READER : minAccessRole;
    return new GoogleCalendarRestApi(webClientBuilder, token).list(accessRole);
  }

  public List<OutlookCalendar> listOutlookCalendars(
      User user, OutlookCalendarAccessRole minAccessRole, NitteiContext ctx) {
    String token = outlookOAuthService.getAccessToken(user, ctx);
    if (token == null || token.isBlank()) {
      return null;
    }
    OutlookCalendarAccessRole accessRole =
        minAccessRole == null ? OutlookCalendarAccessRole.READER : minAccessRole;
    return new OutlookCalendarRestApi(webClientBuilder, token).list(accessRole);
  }
}
