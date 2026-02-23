package com.meetsmore.nittei.infra.repos;

import com.meetsmore.nittei.domain.EventRemindersExpansionJob;
import java.time.Instant;
import java.util.List;

public interface EventReminderGenerationJobsRepository {
    void bulkInsert(List<EventRemindersExpansionJob> jobs);
    List<EventRemindersExpansionJob> deleteAllBefore(Instant before);
}
