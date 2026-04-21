package com.meetsmore.nittei.infra.repos.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.meetsmore.nittei.domain.EventRemindersExpansionJob;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class PostgresEventReminderGenerationJobsRepositoryTest
    extends PostgresRepositoryIntegrationTestSupport {

  @Test
  void bulkInsertAndDeleteAllBeforeReturnDueJobsFromPostgres() {
    var accountId = id("00000000-0000-0000-0000-000000000101");
    var userId = id("00000000-0000-0000-0000-000000000102");
    var serviceId = id("00000000-0000-0000-0000-000000000103");
    var calendarId = id("00000000-0000-0000-0000-000000000104");
    var eventId1 = id("00000000-0000-0000-0000-000000000105");
    var eventId2 = id("00000000-0000-0000-0000-000000000106");

    insertAccount(accountId);
    insertUser(accountId, userId);
    insertService(accountId, serviceId);
    insertCalendar(accountId, userId, calendarId);
    insertCalendarEvent(
        accountId,
        userId,
        eventId1,
        calendarId,
        serviceId,
        Instant.parse("2026-04-20T00:00:00Z"),
        Instant.parse("2026-04-20T01:00:00Z"));
    insertCalendarEvent(
        accountId,
        userId,
        eventId2,
        calendarId,
        serviceId,
        Instant.parse("2026-04-21T00:00:00Z"),
        Instant.parse("2026-04-21T01:00:00Z"));
    insertEventReminderVersion(eventId1, 1);
    insertEventReminderVersion(eventId2, 2);

    PostgresEventReminderGenerationJobsRepository repository =
        new PostgresEventReminderGenerationJobsRepository(jdbcTemplate);
    EventRemindersExpansionJob dueJob =
        new EventRemindersExpansionJob(eventId1, Instant.parse("2026-04-20T09:00:00Z"), 1);
    EventRemindersExpansionJob futureJob =
        new EventRemindersExpansionJob(eventId2, Instant.parse("2026-04-21T09:00:00Z"), 2);

    repository.bulkInsert(List.of(dueJob, futureJob));

    List<EventRemindersExpansionJob> deleted =
        repository.deleteAllBefore(Instant.parse("2026-04-20T12:00:00Z"));

    assertEquals(List.of(dueJob), deleted);
    assertEquals(
        List.of(futureJob), repository.deleteAllBefore(Instant.parse("2026-04-22T00:00:00Z")));
  }

  @Test
  void bulkInsertDoesNothingForEmptyInput() {
    PostgresEventReminderGenerationJobsRepository repository =
        new PostgresEventReminderGenerationJobsRepository(jdbcTemplate);

    repository.bulkInsert(List.of());

    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM calendar_event_reminder_generation_jobs", Integer.class);
    assertEquals(0, count);
  }

  @Test
  void deleteAllBeforeReturnsMultipleJobsWithTheirVersions() {
    var accountId = id("00000000-0000-0000-0000-000000000201");
    var userId = id("00000000-0000-0000-0000-000000000202");
    var serviceId = id("00000000-0000-0000-0000-000000000203");
    var calendarId = id("00000000-0000-0000-0000-000000000204");
    var eventId1 = id("00000000-0000-0000-0000-000000000205");
    var eventId2 = id("00000000-0000-0000-0000-000000000206");

    insertAccount(accountId);
    insertUser(accountId, userId);
    insertService(accountId, serviceId);
    insertCalendar(accountId, userId, calendarId);
    insertCalendarEvent(
        accountId,
        userId,
        eventId1,
        calendarId,
        serviceId,
        Instant.parse("2026-04-22T00:00:00Z"),
        Instant.parse("2026-04-22T01:00:00Z"));
    insertCalendarEvent(
        accountId,
        userId,
        eventId2,
        calendarId,
        serviceId,
        Instant.parse("2026-04-22T02:00:00Z"),
        Instant.parse("2026-04-22T03:00:00Z"));
    insertEventReminderVersion(eventId1, 4);
    insertEventReminderVersion(eventId2, 7);

    PostgresEventReminderGenerationJobsRepository repository =
        new PostgresEventReminderGenerationJobsRepository(jdbcTemplate);
    EventRemindersExpansionJob job1 =
        new EventRemindersExpansionJob(eventId1, Instant.parse("2026-04-22T08:00:00Z"), 4);
    EventRemindersExpansionJob job2 =
        new EventRemindersExpansionJob(eventId2, Instant.parse("2026-04-22T08:05:00Z"), 7);

    repository.bulkInsert(List.of(job1, job2));

    List<EventRemindersExpansionJob> deleted =
        repository.deleteAllBefore(Instant.parse("2026-04-22T08:05:00Z")).stream()
            .sorted(Comparator.comparing(EventRemindersExpansionJob::timestamp))
            .toList();

    assertEquals(List.of(job1, job2), deleted);
  }
}
