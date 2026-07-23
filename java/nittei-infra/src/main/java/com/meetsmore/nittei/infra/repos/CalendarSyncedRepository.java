package com.meetsmore.nittei.infra.repos;

import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.SyncedCalendar;
import java.util.List;

public interface CalendarSyncedRepository {
  void insert(SyncedCalendar calendar);

  void delete(SyncedCalendar calendar);

  List<SyncedCalendar> findByCalendar(ID calendarId);
}
