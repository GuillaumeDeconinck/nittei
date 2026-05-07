package com.meetsmore.nittei.api.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetsmore.nittei.api.error.NitteiApiException;
import com.meetsmore.nittei.api.error.NitteiErrorCode;
import com.meetsmore.nittei.api.shared.auth.RequestContextResolver;
import com.meetsmore.nittei.api.structs.event.EventApi;
import com.meetsmore.nittei.api.structs.event.EventDtos;
import com.meetsmore.nittei.domain.Calendar;
import com.meetsmore.nittei.domain.CalendarEvent;
import com.meetsmore.nittei.domain.CalendarEventStatus;
import com.meetsmore.nittei.domain.EventExpansion;
import com.meetsmore.nittei.domain.EventInstance;
import com.meetsmore.nittei.domain.EventWithInstances;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.Metadata;
import com.meetsmore.nittei.domain.RRuleOptions;
import com.meetsmore.nittei.domain.TimeSpan;
import com.meetsmore.nittei.infra.context.NitteiContext;
import com.meetsmore.nittei.infra.repos.query.MetadataFindQuery;
import com.meetsmore.nittei.infra.repos.query.SearchEventsForUserParams;
import com.meetsmore.nittei.infra.repos.query.SearchEventsParams;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@org.springframework.stereotype.Service
public class EventApplicationService {

  private final NitteiContext ctx;
  private final RequestContextResolver auth;
  private final ObjectMapper objectMapper;

  public EventApplicationService(
      NitteiContext ctx, RequestContextResolver auth, ObjectMapper objectMapper) {
    this.ctx = ctx;
    this.auth = auth;
    this.objectMapper = objectMapper;
  }

  public ResponseEntity<EventApi.CalendarEventResponse> createEventAdmin(
      HttpHeaders headers, String userId, EventApi.CreateEventRequestBody body) {
    var account = auth.requireAdminAccount(headers, ctx);
    ID uid = auth.parseId(userId, "Malformed userId");
    auth.requireAccountUser(account, uid, ctx);
    CalendarEvent event = newEvent(uid, account.id(), body);
    ctx.repos().events().insert(event);
    return ResponseEntity.status(201).body(EventApi.CalendarEventResponse.from(event));
  }

  public ResponseEntity<EventApi.CreateBatchEventsAPIResponse> createBatchEventsAdmin(
      HttpHeaders headers, String userId, EventApi.CreateBatchEventsRequestBody body) {
    var account = auth.requireAdminAccount(headers, ctx);
    ID uid = auth.parseId(userId, "Malformed userId");
    auth.requireAccountUser(account, uid, ctx);
    List<CalendarEvent> events =
        body.events().stream().map(e -> newEvent(uid, account.id(), e)).toList();
    ctx.repos().events().insertMany(events);
    return ResponseEntity.status(201)
        .body(
            new EventApi.CreateBatchEventsAPIResponse(
                events.stream().map(EventDtos.CalendarEventDTO::from).toList()));
  }

  public ResponseEntity<EventApi.SearchEventsAPIResponse> searchEventsAdmin(
      HttpHeaders headers, EventApi.SearchEventsRequestBody body) {
    var account = auth.requireAdminAccount(headers, ctx);
    var filter = body == null ? null : body.filter();
    if (filter == null || filter.userId() == null) {
      throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, "filter.userId is required");
    }
    auth.requireAccountUser(account, filter.userId(), ctx);
    List<CalendarEvent> events =
        ctx.repos()
            .events()
            .searchEventsForUser(
                new SearchEventsForUserParams(
                    filter.userId(),
                    filter.calendarIds(),
                    new SearchEventsParams(
                        filter.eventUid(),
                        null,
                        filter.externalId(),
                        filter.externalParentId(),
                        filter.startTime(),
                        filter.endTime(),
                        filter.status(),
                        filter.eventType(),
                        filter.recurringEventUid(),
                        filter.originalStartTime(),
                        filter.recurrence(),
                        filter.metadata(),
                        filter.createdAt(),
                        filter.updatedAt()),
                    body == null ? null : body.sort(),
                    body == null ? null : body.limit()));
    return ResponseEntity.ok(
        new EventApi.SearchEventsAPIResponse(
            events.stream().map(EventDtos.CalendarEventDTO::from).toList()));
  }

  public ResponseEntity<EventApi.GetEventsByMetaAPIResponse> getEventsByMetaAdmin(
      HttpHeaders headers, EventApi.GetEventsByMetaQueryParams query) {
    var account = auth.requireAdminAccount(headers, ctx);
    List<CalendarEvent> events =
        ctx.repos()
            .events()
            .findByMetadata(
                new MetadataFindQuery(
                    Metadata.of(query.key(), query.value()),
                    query.skip() == null ? 0 : query.skip(),
                    query.limit() == null ? 20 : query.limit(),
                    account.id()));
    return ResponseEntity.ok(
        new EventApi.GetEventsByMetaAPIResponse(
            events.stream().map(EventDtos.CalendarEventDTO::from).toList()));
  }

  public ResponseEntity<EventApi.GetEventsForUsersInTimeSpanAPIResponse> getEventsInTimespanAdmin(
      HttpHeaders headers, EventApi.GetEventsForUsersInTimeSpanBody body) {
    var account = auth.requireAdminAccount(headers, ctx);
    for (ID userId : body.userIds()) {
      auth.requireAccountUser(account, userId, ctx);
    }

    TimeSpan span = new TimeSpan(body.startTime(), body.endTime());
    List<CalendarEvent> events =
        new ArrayList<>(
            ctx.repos()
                .events()
                .findEventsForUsersForTimespan(
                    body.userIds(),
                    span,
                    Boolean.TRUE.equals(body.includeTentative()),
                    Boolean.TRUE.equals(body.includeNonBusy())));
    List<CalendarEvent> recurringEvents =
        ctx.repos()
            .events()
            .findRecurringEventsForUsersForTimespan(
                body.userIds(),
                span,
                Boolean.TRUE.equals(body.includeTentative()),
                Boolean.TRUE.equals(body.includeNonBusy()));
    events.addAll(recurringEvents);
    List<ID> recurringIds = recurringEvents.stream().map(CalendarEvent::id).toList();
    events.addAll(ctx.repos().events().findByRecurringEventIdsForTimespan(recurringIds, span));

    return ResponseEntity.ok(
        new EventApi.GetEventsForUsersInTimeSpanAPIResponse(
            events.stream().map(this::toEventWithSingleInstanceDto).toList()));
  }

  public ResponseEntity<EventApi.GetEventsByCalendarsAPIResponse> getEventsByCalendarsAdmin(
      HttpHeaders headers, String userId, EventApi.GetEventsByCalendarsQueryParams query) {
    var account = auth.requireAdminAccount(headers, ctx);
    ID uid = auth.parseId(userId, "Malformed userId");
    auth.requireAccountUser(account, uid, ctx);
    TimeSpan span = new TimeSpan(query.startTime(), query.endTime());
    List<CalendarEvent> events =
        query.calendarIds() == null || query.calendarIds().isEmpty()
            ? ctx.repos().events().findEventsForUsersForTimespan(List.of(uid), span, false, false)
            : ctx.repos().events().findByCalendars(query.calendarIds(), span);
    return ResponseEntity.ok(
        EventApi.GetEventsByCalendarsAPIResponse.from(
            events.stream()
                .map(
                    e ->
                        new EventWithInstances(
                            e, List.of(new EventInstance(e.startTime(), e.endTime(), e.busy()))))
                .toList()));
  }

  public ResponseEntity<EventApi.GetEventsByExternalIdAPIResponse> getEventByExternalIdAdmin(
      HttpHeaders headers, String externalId) {
    var account = auth.requireAdminAccount(headers, ctx);
    List<CalendarEvent> events = ctx.repos().events().getByExternalId(account.id(), externalId);
    return ResponseEntity.ok(
        new EventApi.GetEventsByExternalIdAPIResponse(
            events.stream().map(EventDtos.CalendarEventDTO::from).toList()));
  }

  public ResponseEntity<EventApi.CalendarEventResponse> getEventAdmin(
      HttpHeaders headers, String eventId) {
    var account = auth.requireAdminAccount(headers, ctx);
    CalendarEvent event =
        auth.requireAccountEvent(account, auth.parseId(eventId, "Malformed eventId"), ctx);
    return ResponseEntity.ok(EventApi.CalendarEventResponse.from(event));
  }

  public ResponseEntity<EventApi.CalendarEventResponse> updateEventAdmin(
      HttpHeaders headers, String eventId, JsonNode rawBody) {
    var account = auth.requireAdminAccount(headers, ctx);
    CalendarEvent current =
        auth.requireAccountEvent(account, auth.parseId(eventId, "Malformed eventId"), ctx);
    EventApi.UpdateEventRequestBody body =
        objectMapper.convertValue(rawBody, EventApi.UpdateEventRequestBody.class);
    CalendarEvent updated = mergeEvent(current, body, rawBody);
    ctx.repos().events().save(updated);
    return ResponseEntity.ok(EventApi.CalendarEventResponse.from(updated));
  }

  public ResponseEntity<EventApi.CalendarEventResponse> deleteEventAdmin(
      HttpHeaders headers, String eventId) {
    var account = auth.requireAdminAccount(headers, ctx);
    ID eid = auth.parseId(eventId, "Malformed eventId");
    CalendarEvent current = auth.requireAccountEvent(account, eid, ctx);
    ctx.repos().events().delete(eid);
    return ResponseEntity.ok(EventApi.CalendarEventResponse.from(current));
  }

  public ResponseEntity<EventApi.GetEventInstancesAPIResponse> getEventInstancesAdmin(
      HttpHeaders headers, String eventId, EventApi.GetEventInstancesQueryParams query) {
    var account = auth.requireAdminAccount(headers, ctx);
    CalendarEvent event =
        auth.requireAccountEvent(account, auth.parseId(eventId, "Malformed eventId"), ctx);
    List<EventInstance> instances = resolveInstances(event.id(), event.userId(), query);
    return ResponseEntity.ok(
        new EventApi.GetEventInstancesAPIResponse(
            EventDtos.CalendarEventDTO.from(event), instances));
  }

  public ResponseEntity<Void> deleteManyEventsAdmin(
      HttpHeaders headers, EventApi.DeleteManyEventsRequestBody body) {
    var account = auth.requireAdminAccount(headers, ctx);
    if (body == null) {
      return ResponseEntity.ok().build();
    }
    if (body.eventIds() != null && !body.eventIds().isEmpty()) {
      for (ID id : body.eventIds()) {
        auth.requireAccountEvent(account, id, ctx);
      }
      ctx.repos().events().deleteMany(body.eventIds());
    }
    if (body.externalIds() != null && !body.externalIds().isEmpty()) {
      List<ID> ids =
          ctx.repos().events().findManyByExternalIds(account.id(), body.externalIds()).stream()
              .map(CalendarEvent::id)
              .toList();
      if (!ids.isEmpty()) {
        ctx.repos().events().deleteMany(ids);
      }
    }
    return ResponseEntity.ok().build();
  }

  public ResponseEntity<EventApi.CalendarEventResponse> createEvent(
      HttpHeaders headers, EventApi.CreateEventRequestBody body) {
    var userCtx = auth.requireUserContext(headers, ctx);
    CalendarEvent event = newEvent(userCtx.user().id(), userCtx.account().id(), body);
    ctx.repos().events().insert(event);
    return ResponseEntity.status(201).body(EventApi.CalendarEventResponse.from(event));
  }

  public ResponseEntity<EventApi.CalendarEventResponse> getEvent(
      HttpHeaders headers, String eventId) {
    var userCtx = auth.requireUserContext(headers, ctx);
    CalendarEvent event =
        ctx.repos()
            .events()
            .find(auth.parseId(eventId, "Malformed eventId"))
            .filter(e -> e.userId().equals(userCtx.user().id()))
            .orElseThrow(
                () ->
                    new NitteiApiException(
                        NitteiErrorCode.NOT_FOUND,
                        "The calendar event with id: " + eventId + ", was not found"));
    return ResponseEntity.ok(EventApi.CalendarEventResponse.from(event));
  }

  public ResponseEntity<EventApi.CalendarEventResponse> updateEvent(
      HttpHeaders headers, String eventId, JsonNode rawBody) {
    var userCtx = auth.requireUserContext(headers, ctx);
    CalendarEvent current =
        ctx.repos()
            .events()
            .find(auth.parseId(eventId, "Malformed eventId"))
            .filter(e -> e.userId().equals(userCtx.user().id()))
            .orElseThrow(
                () ->
                    new NitteiApiException(
                        NitteiErrorCode.NOT_FOUND,
                        "The calendar event with id: " + eventId + ", was not found"));
    EventApi.UpdateEventRequestBody body =
        objectMapper.convertValue(rawBody, EventApi.UpdateEventRequestBody.class);
    CalendarEvent updated = mergeEvent(current, body, rawBody);
    ctx.repos().events().save(updated);
    return ResponseEntity.ok(EventApi.CalendarEventResponse.from(updated));
  }

  public ResponseEntity<EventApi.CalendarEventResponse> deleteEvent(
      HttpHeaders headers, String eventId) {
    var userCtx = auth.requireUserContext(headers, ctx);
    ID eid = auth.parseId(eventId, "Malformed eventId");
    CalendarEvent current =
        ctx.repos()
            .events()
            .find(eid)
            .filter(e -> e.userId().equals(userCtx.user().id()))
            .orElseThrow(
                () ->
                    new NitteiApiException(
                        NitteiErrorCode.NOT_FOUND,
                        "The calendar event with id: " + eventId + ", was not found"));
    ctx.repos().events().delete(eid);
    return ResponseEntity.ok(EventApi.CalendarEventResponse.from(current));
  }

  public ResponseEntity<EventApi.GetEventInstancesAPIResponse> getEventInstances(
      HttpHeaders headers, String eventId, EventApi.GetEventInstancesQueryParams query) {
    var userCtx = auth.requireUserContext(headers, ctx);
    CalendarEvent event =
        ctx.repos()
            .events()
            .find(auth.parseId(eventId, "Malformed eventId"))
            .filter(e -> e.userId().equals(userCtx.user().id()))
            .orElseThrow(
                () ->
                    new NitteiApiException(
                        NitteiErrorCode.NOT_FOUND,
                        "The calendar event with id: " + eventId + ", was not found"));
    List<EventInstance> instances = resolveInstances(event.id(), userCtx.user().id(), query);
    return ResponseEntity.ok(
        new EventApi.GetEventInstancesAPIResponse(
            EventDtos.CalendarEventDTO.from(event), instances));
  }

  private CalendarEvent newEvent(ID userId, ID accountId, EventApi.CreateEventRequestBody body) {
    if (body == null || body.calendarId() == null || body.startTime() == null) {
      throw new NitteiApiException(
          NitteiErrorCode.BAD_CLIENT_DATA, "Missing required event fields");
    }
    long duration = body.duration();
    Instant end = body.startTime().plusMillis(duration);
    return new CalendarEvent(
        ID.random(),
        body.externalParentId(),
        body.externalId(),
        body.title(),
        body.description(),
        body.eventType(),
        body.location(),
        body.allDay() != null && body.allDay(),
        body.status() == null ? CalendarEventStatus.TENTATIVE : body.status(),
        body.startTime(),
        duration,
        body.busy() != null && body.busy(),
        end,
        body.created() == null ? Instant.now() : body.created(),
        body.updated() == null ? Instant.now() : body.updated(),
        body.recurrence(),
        body.exdates() == null ? List.of() : body.exdates(),
        computeRecurringUntil(body.recurrence(), duration),
        body.recurringEventId(),
        body.originalStartTime(),
        body.calendarId(),
        userId,
        accountId,
        body.reminders() == null ? List.of() : body.reminders(),
        body.serviceId(),
        body.metadata());
  }

  private CalendarEvent mergeEvent(
      CalendarEvent current, EventApi.UpdateEventRequestBody body, JsonNode rawBody) {
    boolean startChanged =
        body.startTime() != null && !body.startTime().equals(current.startTime());
    Instant start = body.startTime() == null ? current.startTime() : body.startTime();
    long duration =
        body.duration() == null
            ? Objects.requireNonNullElse(current.duration(), 0L)
            : body.duration();
    Instant end = start.plusMillis(duration);

    List<Instant> exdates = startChanged ? List.of() : current.exdates();
    if (hasField(rawBody, "exdates")) {
      exdates = body.exdates() == null ? List.of() : body.exdates();
    }

    boolean recurrenceProvided = hasField(rawBody, "recurrence");
    RRuleOptions recurrence = recurrenceProvided ? body.recurrence() : current.recurrence();
    Instant recurringUntil =
        recurrenceProvided
            ? computeRecurringUntil(recurrence, duration)
            : (startChanged || body.duration() != null
                ? computeRecurringUntil(current.recurrence(), duration)
                : current.recurringUntil());

    return new CalendarEvent(
        current.id(),
        hasField(rawBody, "externalParentId")
            ? body.externalParentId()
            : current.externalParentId(),
        hasField(rawBody, "externalId") ? body.externalId() : current.externalId(),
        hasField(rawBody, "title") ? body.title() : current.title(),
        hasField(rawBody, "description") ? body.description() : current.description(),
        hasField(rawBody, "eventType") ? body.eventType() : current.eventType(),
        hasField(rawBody, "location") ? body.location() : current.location(),
        body.allDay() != null ? body.allDay() : current.allDay(),
        body.status() != null ? body.status() : current.status(),
        start,
        duration,
        body.busy() != null ? body.busy() : current.busy(),
        end,
        body.created() != null ? body.created() : current.created(),
        body.updated() != null ? body.updated() : Instant.now(),
        recurrence,
        exdates,
        recurringUntil,
        hasField(rawBody, "recurringEventId")
            ? body.recurringEventId()
            : current.recurringEventId(),
        hasField(rawBody, "originalStartTime")
            ? body.originalStartTime()
            : current.originalStartTime(),
        current.calendarId(),
        current.userId(),
        current.accountId(),
        hasField(rawBody, "reminders")
            ? (body.reminders() == null ? List.of() : body.reminders())
            : current.reminders(),
        hasField(rawBody, "serviceId") ? body.serviceId() : current.serviceId(),
        hasField(rawBody, "metadata") ? body.metadata() : current.metadata());
  }

  private EventDtos.EventWithInstancesDTO toEventWithSingleInstanceDto(CalendarEvent event) {
    return new EventDtos.EventWithInstancesDTO(
        EventDtos.CalendarEventDTO.from(event),
        List.of(new EventInstance(event.startTime(), event.endTime(), event.busy())));
  }

  private boolean hasField(JsonNode node, String fieldName) {
    return node != null && node.has(fieldName);
  }

  private Instant computeRecurringUntil(RRuleOptions recurrence, long durationMs) {
    if (recurrence == null || recurrence.until() == null) {
      return null;
    }
    return recurrence.until().plusMillis(durationMs);
  }

  private List<EventInstance> resolveInstances(
      ID eventId, ID userId, EventApi.GetEventInstancesQueryParams query) {
    CalendarEvent main =
        ctx.repos()
            .events()
            .find(eventId)
            .filter(e -> e.userId().equals(userId))
            .orElseThrow(
                () ->
                    new NitteiApiException(
                        NitteiErrorCode.NOT_FOUND,
                        "The calendar event with id: " + eventId.value() + ", was not found"));
    Calendar calendar =
        ctx.repos()
            .calendars()
            .find(main.calendarId())
            .orElseThrow(
                () ->
                    new NitteiApiException(
                        NitteiErrorCode.NOT_FOUND,
                        "The calendar with id: " + main.calendarId().value() + ", was not found"));

    List<Instant> exceptionStartTimes =
        ctx.repos().events().findByIdAndRecurringEventId(eventId).stream()
            .filter(e -> e.recurringEventId() != null && eventId.equals(e.recurringEventId()))
            .map(CalendarEvent::originalStartTime)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    return EventExpansion.expandEventAndRemoveExceptions(
        calendar, main, exceptionStartTimes, new TimeSpan(query.startTime(), query.endTime()));
  }
}
