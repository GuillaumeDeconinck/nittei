package com.meetsmore.nittei.domain;

public record SyncedCalendarEvent(
    ID eventId,
    ID calendarId,
    ID userId,
    String extEventId,
    String extCalendarId,
    IntegrationProvider provider
) {
}
