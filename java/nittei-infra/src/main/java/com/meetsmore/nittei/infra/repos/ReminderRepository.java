package com.meetsmore.nittei.infra.repos;

import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.Reminder;
import java.time.Instant;
import java.util.List;

public interface ReminderRepository {
  void bulkInsert(List<Reminder> reminders);

  long initVersion(ID eventId);

  long incVersion(ID eventId);

  List<Reminder> deleteAllBefore(Instant before);
}
