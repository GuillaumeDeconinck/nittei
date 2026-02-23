package com.meetsmore.nittei.infra.repos.query;

import com.meetsmore.nittei.domain.DateTimeQuery;
import com.meetsmore.nittei.domain.IDQuery;
import com.meetsmore.nittei.domain.RecurrenceQuery;
import com.meetsmore.nittei.domain.StringQuery;

public record SearchEventsParams(
    IDQuery eventUid,
    IDQuery userUid,
    StringQuery externalId,
    StringQuery externalParentId,
    DateTimeQuery startTime,
    DateTimeQuery endTime,
    StringQuery status,
    StringQuery eventType,
    IDQuery recurringEventUid,
    DateTimeQuery originalStartTime,
    RecurrenceQuery recurrence,
    Object metadata,
    DateTimeQuery createdAt,
    DateTimeQuery updatedAt
) {
}
