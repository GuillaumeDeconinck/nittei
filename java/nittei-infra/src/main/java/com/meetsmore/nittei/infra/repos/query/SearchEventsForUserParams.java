package com.meetsmore.nittei.infra.repos.query;

import com.meetsmore.nittei.domain.CalendarEventSort;
import com.meetsmore.nittei.domain.ID;
import java.util.List;

public record SearchEventsForUserParams(
    ID userId,
    List<ID> calendarIds,
    SearchEventsParams searchEventsParams,
    CalendarEventSort sort,
    Integer limit) {}
