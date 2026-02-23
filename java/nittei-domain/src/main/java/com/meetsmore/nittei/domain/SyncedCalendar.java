package com.meetsmore.nittei.domain;

public record SyncedCalendar(IntegrationProvider provider, ID calendarId, ID userId, String extCalendarId) {
}
