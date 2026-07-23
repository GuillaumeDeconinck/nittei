package com.meetsmore.nittei.domain;

import java.time.Instant;
import java.util.List;

public record CalendarEvent(
    ID id,
    String externalParentId,
    String externalId,
    String title,
    String description,
    String eventType,
    String location,
    boolean allDay,
    CalendarEventStatus status,
    Instant startTime,
    Long duration,
    boolean busy,
    Instant endTime,
    Instant created,
    Instant updated,
    RRuleOptions recurrence,
    List<Instant> exdates,
    Instant recurringUntil,
    ID recurringEventId,
    Instant originalStartTime,
    ID calendarId,
    ID userId,
    ID accountId,
    List<CalendarEventReminder> reminders,
    ID serviceId,
    Object metadata) {}
