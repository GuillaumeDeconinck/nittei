package com.meetsmore.nittei.api.structs.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.meetsmore.nittei.api.shared.jackson.StrictIdDeserializer;
import com.meetsmore.nittei.domain.CalendarEvent;
import com.meetsmore.nittei.domain.CalendarEventReminder;
import com.meetsmore.nittei.domain.CalendarEventSort;
import com.meetsmore.nittei.domain.CalendarEventStatus;
import com.meetsmore.nittei.domain.DateTimeQuery;
import com.meetsmore.nittei.domain.EventInstance;
import com.meetsmore.nittei.domain.EventWithInstances;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.IDQuery;
import com.meetsmore.nittei.domain.RRuleOptions;
import com.meetsmore.nittei.domain.RecurrenceQuery;
import com.meetsmore.nittei.domain.StringQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public final class EventApi {

    private EventApi() {
    }

    public record CalendarEventResponse(EventDtos.CalendarEventDTO event) {
        public static CalendarEventResponse from(CalendarEvent event) {
            return new CalendarEventResponse(EventDtos.CalendarEventDTO.from(event));
        }
    }

    public record UserPathParams(ID userId) {
    }

    public record EventPathParams(ID eventId) {
    }

    public record CreateEventRequestBody(
        @JsonDeserialize(using = StrictIdDeserializer.class)
        @NotNull ID calendarId,
        String title,
        String description,
        String eventType,
        String externalParentId,
        String externalId,
        String location,
        CalendarEventStatus status,
        Boolean allDay,
        @NotNull Instant startTime,
        @Positive long duration,
        Boolean busy,
        RRuleOptions recurrence,
        List<Instant> exdates,
        ID recurringEventId,
        Instant originalStartTime,
        List<CalendarEventReminder> reminders,
        ID serviceId,
        Object metadata,
        Instant created,
        Instant updated
    ) {
    }

    public record CreateBatchEventsRequestBody(
        @NotEmpty List<@Valid CreateEventRequestBody> events
    ) {
    }

    public record CreateBatchEventsAPIResponse(List<EventDtos.CalendarEventDTO> events) {
    }

    public record DeleteManyEventsRequestBody(List<ID> eventIds, List<String> externalIds) {
    }

    public record GetEventInstancesQueryParams(Instant startTime, Instant endTime) {
    }

    public record GetEventInstancesAPIResponse(EventDtos.CalendarEventDTO event, List<EventInstance> instances) {
    }

    public record GetEventsByExternalIdPathParams(String externalId) {
    }

    public record GetEventsByExternalIdAPIResponse(List<EventDtos.CalendarEventDTO> events) {
    }

    public record GetEventsByCalendarsQueryParams(List<ID> calendarIds, Instant startTime, Instant endTime) {
    }

    public record GetEventsByCalendarsAPIResponse(List<EventDtos.EventWithInstancesDTO> events) {
        public static GetEventsByCalendarsAPIResponse from(List<EventWithInstances> events) {
            List<EventDtos.EventWithInstancesDTO> items = events.stream()
                .map(e -> new EventDtos.EventWithInstancesDTO(EventDtos.CalendarEventDTO.from(e.event()), e.instances()))
                .collect(Collectors.toList());
            return new GetEventsByCalendarsAPIResponse(items);
        }
    }

    public record GetEventsForUsersInTimeSpanBody(
        @NotEmpty List<ID> userIds,
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        Boolean generateInstancesForRecurring,
        Boolean includeTentative,
        Boolean includeNonBusy
    ) {
    }

    public record GetEventsForUsersInTimeSpanAPIResponse(List<EventDtos.EventWithInstancesDTO> events) {
    }

    public record SearchEventsRequestBody(SearchEventsRequestBodyFilter filter, CalendarEventSort sort, Integer limit) {
    }

    public record SearchEventsRequestBodyFilter(
        ID userId,
        IDQuery eventUid,
        List<ID> calendarIds,
        StringQuery externalId,
        StringQuery externalParentId,
        DateTimeQuery startTime,
        DateTimeQuery endTime,
        StringQuery eventType,
        StringQuery status,
        IDQuery recurringEventUid,
        DateTimeQuery originalStartTime,
        RecurrenceQuery recurrence,
        Object metadata,
        DateTimeQuery createdAt,
        DateTimeQuery updatedAt
    ) {
    }

    public record SearchEventsAPIResponse(List<EventDtos.CalendarEventDTO> events) {
    }

    public record GetEventsByMetaQueryParams(String key, String value, Integer skip, Integer limit) {
    }

    public record GetEventsByMetaAPIResponse(List<EventDtos.CalendarEventDTO> events) {
    }

    public record UpdateEventRequestBody(
        String title,
        String description,
        String eventType,
        String externalParentId,
        String externalId,
        String location,
        CalendarEventStatus status,
        Boolean allDay,
        Instant startTime,
        Long duration,
        Boolean busy,
        RRuleOptions recurrence,
        ID serviceId,
        List<Instant> exdates,
        ID recurringEventId,
        Instant originalStartTime,
        List<CalendarEventReminder> reminders,
        Object metadata,
        Instant created,
        Instant updated
    ) {
    }

    public record AccountEventReminder(CalendarEvent event, String identifier) {
    }

    public record AccountEventRemindersDTO(EventDtos.CalendarEventDTO event, String identifier) {
        public static AccountEventRemindersDTO from(AccountEventReminder value) {
            return new AccountEventRemindersDTO(EventDtos.CalendarEventDTO.from(value.event()), value.identifier());
        }
    }

    public record AccountReminders(List<AccountEventReminder> reminders) {
    }

    public record AccountRemindersDTO(List<AccountEventRemindersDTO> reminders) {
        public static AccountRemindersDTO from(AccountReminders value) {
            List<AccountEventRemindersDTO> items = value.reminders().stream().map(AccountEventRemindersDTO::from).toList();
            return new AccountRemindersDTO(items);
        }
    }
}
