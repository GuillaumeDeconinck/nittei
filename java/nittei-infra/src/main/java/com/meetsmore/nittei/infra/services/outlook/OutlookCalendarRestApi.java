package com.meetsmore.nittei.infra.services.outlook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meetsmore.nittei.domain.CalendarEvent;
import com.meetsmore.nittei.domain.EventInstance;
import com.meetsmore.nittei.domain.providers.outlook.OutlookCalendar;
import com.meetsmore.nittei.domain.providers.outlook.OutlookCalendarAccessRole;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

public class OutlookCalendarRestApi {

  private static final Logger log = LoggerFactory.getLogger(OutlookCalendarRestApi.class);
  private static final String API_BASE_URL = "https://graph.microsoft.com/v1.0";

  private final WebClient webClient;

  public OutlookCalendarRestApi(WebClient.Builder webClientBuilder, String accessToken) {
    this.webClient =
        webClientBuilder
            .defaultHeader("authorization", "Bearer " + accessToken)
            .baseUrl(API_BASE_URL)
            .build();
  }

  public List<OutlookCalendar> list(OutlookCalendarAccessRole minAccessRole) {
    OutlookCalendarListResponse response =
        webClient
            .get()
            .uri("/me/calendars")
            .retrieve()
            .bodyToMono(OutlookCalendarListResponse.class)
            .block();
    if (response == null || response.value() == null) {
      return List.of();
    }
    return switch (minAccessRole) {
      case READER -> response.value();
      case WRITER -> response.value().stream().filter(OutlookCalendar::canEdit).toList();
    };
  }

  public void remove(String calendarId, String eventId) {
    webClient
        .delete()
        .uri("/me/calendars/{calendarId}/events/{eventId}", calendarId, eventId)
        .retrieve()
        .toBodilessEntity()
        .block();
  }

  public Map<String, Object> update(String calendarId, String eventId, CalendarEvent event) {
    log.debug("Outlook update event request for calendar {} event {}", calendarId, eventId);
    throw new UnsupportedOperationException("Outlook update mapping not implemented yet");
  }

  public Map<String, Object> insert(String calendarId, CalendarEvent event) {
    log.debug("Outlook insert event request for calendar {}", calendarId);
    throw new UnsupportedOperationException("Outlook insert mapping not implemented yet");
  }

  public List<EventInstance> freebusy(List<String> calendars, Instant start, Instant end) {
    throw new UnsupportedOperationException("Outlook freebusy mapping not implemented yet");
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record OutlookCalendarListResponse(@JsonProperty("value") List<OutlookCalendar> value) {}
}
