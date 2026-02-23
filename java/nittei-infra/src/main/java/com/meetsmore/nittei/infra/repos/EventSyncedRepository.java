package com.meetsmore.nittei.infra.repos;

import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.SyncedCalendarEvent;
import java.util.List;

public interface EventSyncedRepository {
    void insert(SyncedCalendarEvent event);
    List<SyncedCalendarEvent> findByEvent(ID eventId);
}
