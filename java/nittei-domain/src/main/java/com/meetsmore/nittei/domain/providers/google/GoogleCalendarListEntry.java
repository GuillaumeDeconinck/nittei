package com.meetsmore.nittei.domain.providers.google;

public record GoogleCalendarListEntry(
    String id,
    GoogleCalendarAccessRole accessRole,
    String summary,
    String summaryOverride,
    String description,
    String location,
    String timeZone,
    String colorId,
    String backgroundColor,
    String foregroundColor,
    Boolean hidden,
    Boolean selected,
    Boolean primary,
    Boolean deleted) {}
