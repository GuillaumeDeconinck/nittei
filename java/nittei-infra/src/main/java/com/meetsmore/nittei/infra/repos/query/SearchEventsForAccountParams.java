package com.meetsmore.nittei.infra.repos.query;

import com.meetsmore.nittei.domain.CalendarEventSort;
import com.meetsmore.nittei.domain.ID;

public record SearchEventsForAccountParams(
    ID accountId,
    SearchEventsParams searchEventsParams,
    CalendarEventSort sort,
    Integer limit
) {
}
