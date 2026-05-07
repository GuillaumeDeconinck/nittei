package com.meetsmore.nittei.api.structs.calendar;

import com.fasterxml.jackson.annotation.JsonValue;
import com.meetsmore.nittei.api.structs.event.EventDtos;
import com.meetsmore.nittei.domain.Calendar;
import com.meetsmore.nittei.domain.EventInstance;
import com.meetsmore.nittei.domain.EventWithInstances;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.IntegrationProvider;
import com.meetsmore.nittei.domain.providers.google.GoogleCalendarAccessRole;
import com.meetsmore.nittei.domain.providers.google.GoogleCalendarListEntry;
import com.meetsmore.nittei.domain.providers.outlook.OutlookCalendar;
import com.meetsmore.nittei.domain.providers.outlook.OutlookCalendarAccessRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class CalendarApi {

  private CalendarApi() {}

  public record CalendarResponse(CalendarDtos.CalendarDTO calendar) {
    public static CalendarResponse from(Calendar calendar) {
      return new CalendarResponse(CalendarDtos.CalendarDTO.from(calendar));
    }
  }

  public record GetCalendarsByUserQueryParams(String key) {}

  public record UserPathParams(ID userId) {}

  public record CalendarPathParams(ID calendarId) {}

  public record GetCalendarsByUserAPIResponse(List<CalendarDtos.CalendarDTO> calendars) {}

  public record CreateCalendarRequestBody(
      @NotBlank String timezone, DayOfWeek weekStart, String name, String key, Object metadata) {}

  public record AddSyncCalendarRequestBody(
      @NotNull IntegrationProvider provider,
      @NotNull ID calendarId,
      @NotBlank String extCalendarId) {}

  public record RemoveSyncCalendarRequestBody(
      @NotNull IntegrationProvider provider,
      @NotNull ID calendarId,
      @NotBlank String extCalendarId) {}

  public record GetCalendarEventsIcalQueryParams(Instant startTime, Instant endTime) {}

  public record GetCalendarEventsQueryParams(Instant startTime, Instant endTime) {}

  public record GetCalendarEventsAPIResponse(
      CalendarDtos.CalendarDTO calendar, List<EventDtos.EventWithInstancesDTO> events) {
    public static GetCalendarEventsAPIResponse from(
        Calendar calendar, List<EventWithInstances> events) {
      List<EventDtos.EventWithInstancesDTO> items =
          events.stream()
              .map(
                  e ->
                      new EventDtos.EventWithInstancesDTO(
                          EventDtos.CalendarEventDTO.from(e.event()), e.instances()))
              .collect(Collectors.toList());
      return new GetCalendarEventsAPIResponse(CalendarDtos.CalendarDTO.from(calendar), items);
    }
  }

  public record GetCalendarsByMetaQueryParams(
      String key, String value, Integer skip, Integer limit) {}

  public record GetCalendarsByMetaAPIResponse(List<CalendarDtos.CalendarDTO> calendars) {}

  public record GetGoogleCalendarsQueryParams(GoogleCalendarAccessRole minAccessRole) {}

  public record GetGoogleCalendarsAPIResponse(List<GoogleCalendarListEntry> calendars) {}

  public record GetOutlookCalendarsQueryParams(OutlookCalendarAccessRole minAccessRole) {}

  public record GetOutlookCalendarsAPIResponse(List<OutlookCalendar> calendars) {}

  public record GetUserFreeBusyQueryParams(
      Instant startTime, Instant endTime, List<ID> calendarIds, Boolean includeTentative) {}

  public record GetUserFreeBusyAPIResponse(List<EventInstance> busy, String userId) {}

  public record MultipleFreeBusyRequestBody(
      @NotEmpty List<ID> userIds, @NotNull Instant startTime, @NotNull Instant endTime) {}

  public record MultipleFreeBusyAPIResponse(@JsonValue Map<ID, List<EventInstance>> value) {}

  public record UpdateCalendarSettings(DayOfWeek weekStart, String timezone) {}

  public record UpdateCalendarRequestBody(
      UpdateCalendarSettings settings, String name, Object metadata) {}
}
