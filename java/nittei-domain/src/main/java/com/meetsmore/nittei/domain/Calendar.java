package com.meetsmore.nittei.domain;

public record Calendar(
    ID id,
    ID userId,
    ID accountId,
    String name,
    String key,
    CalendarSettings settings,
    Object metadata
) {
}
