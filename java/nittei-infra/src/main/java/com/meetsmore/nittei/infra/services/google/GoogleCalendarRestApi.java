package com.meetsmore.nittei.infra.services.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meetsmore.nittei.domain.CalendarEvent;
import com.meetsmore.nittei.domain.EventInstance;
import com.meetsmore.nittei.domain.providers.google.GoogleCalendarAccessRole;
import com.meetsmore.nittei.domain.providers.google.GoogleCalendarListEntry;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

public class GoogleCalendarRestApi {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarRestApi.class);
    private static final String GOOGLE_API_BASE_URL = "https://www.googleapis.com/calendar/v3";

    private final WebClient webClient;

    public GoogleCalendarRestApi(WebClient.Builder webClientBuilder, String accessToken) {
        this.webClient = webClientBuilder
            .defaultHeader("authorization", "Bearer " + accessToken)
            .baseUrl(GOOGLE_API_BASE_URL)
            .build();
    }

    public List<EventInstance> freebusy(Map<String, Object> request) {
        throw new UnsupportedOperationException("Google freebusy mapping not implemented yet");
    }

    public Map<String, Object> insert(String calendarId, CalendarEvent event) {
        log.debug("Google insert event request for calendar {}", calendarId);
        throw new UnsupportedOperationException("Google insert mapping not implemented yet");
    }

    public Map<String, Object> update(String calendarId, String eventId, CalendarEvent event) {
        log.debug("Google update event request for calendar {} event {}", calendarId, eventId);
        throw new UnsupportedOperationException("Google update mapping not implemented yet");
    }

    public void remove(String calendarId, String eventId) {
        webClient.delete()
            .uri("/calendars/{calendarId}/events/{eventId}", calendarId, eventId)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    public List<GoogleCalendarListEntry> list(GoogleCalendarAccessRole minAccessRole) {
        String role = switch (minAccessRole) {
            case OWNER -> "owner";
            case WRITER -> "writer";
            case READER -> "reader";
            case FREE_BUSY_READER -> "freeBusyReader";
        };
        GoogleCalendarListResponse response = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/users/me/calendarList")
                .queryParam("minAccessRole", role)
                .build())
            .retrieve()
            .bodyToMono(GoogleCalendarListResponse.class)
            .block();
        if (response == null || response.items() == null) {
            return List.of();
        }
        return response.items().stream()
            .map(item -> new GoogleCalendarListEntry(
                item.id(),
                parseAccessRole(item.accessRole()),
                item.summary(),
                item.summaryOverride(),
                item.description(),
                item.location(),
                item.timeZone(),
                item.colorId(),
                item.backgroundColor(),
                item.foregroundColor(),
                item.hidden(),
                item.selected(),
                item.primary(),
                item.deleted()
            ))
            .toList();
    }

    private GoogleCalendarAccessRole parseAccessRole(String value) {
        if (value == null) {
            return GoogleCalendarAccessRole.READER;
        }
        return switch (value.toLowerCase()) {
            case "owner" -> GoogleCalendarAccessRole.OWNER;
            case "writer" -> GoogleCalendarAccessRole.WRITER;
            case "freebusyreader" -> GoogleCalendarAccessRole.FREE_BUSY_READER;
            default -> GoogleCalendarAccessRole.READER;
        };
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleCalendarListResponse(@JsonProperty("items") List<GoogleCalendarListEntryRaw> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleCalendarListEntryRaw(
        @JsonProperty("id") String id,
        @JsonProperty("accessRole") String accessRole,
        @JsonProperty("summary") String summary,
        @JsonProperty("summaryOverride") String summaryOverride,
        @JsonProperty("description") String description,
        @JsonProperty("location") String location,
        @JsonProperty("timeZone") String timeZone,
        @JsonProperty("colorId") String colorId,
        @JsonProperty("backgroundColor") String backgroundColor,
        @JsonProperty("foregroundColor") String foregroundColor,
        @JsonProperty("hidden") Boolean hidden,
        @JsonProperty("selected") Boolean selected,
        @JsonProperty("primary") Boolean primary,
        @JsonProperty("deleted") Boolean deleted
    ) {
    }
}
