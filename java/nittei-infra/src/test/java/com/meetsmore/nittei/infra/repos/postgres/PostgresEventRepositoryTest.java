package com.meetsmore.nittei.infra.repos.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetsmore.nittei.domain.CalendarEvent;
import com.meetsmore.nittei.domain.CalendarEventReminder;
import com.meetsmore.nittei.domain.CalendarEventStatus;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.Metadata;
import com.meetsmore.nittei.domain.RRuleFrequency;
import com.meetsmore.nittei.domain.RRuleOptions;
import java.time.Instant;
import java.time.Month;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PostgresEventRepositoryTest extends PostgresRepositoryIntegrationTestSupport {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  void insertAndFindRoundTripRecurringAndJsonFields() {
    var accountId = id("00000000-0000-0000-0000-000000000301");
    var userId = id("00000000-0000-0000-0000-000000000302");
    var serviceId = id("00000000-0000-0000-0000-000000000303");
    var calendarId = id("00000000-0000-0000-0000-000000000304");
    var eventId = id("00000000-0000-0000-0000-000000000305");
    var recurringEventId = id("00000000-0000-0000-0000-000000000306");

    insertAccount(accountId);
    insertUser(accountId, userId);
    insertService(accountId, serviceId);
    insertCalendar(accountId, userId, calendarId);

    PostgresEventRepository repository = new PostgresEventRepository(jdbcTemplate, objectMapper);
    CalendarEvent event =
        new CalendarEvent(
            eventId,
            "external-parent-1",
            "external-event-1",
            "Planning Call",
            "Discuss rewrite parity",
            "booking",
            "Tokyo office",
            false,
            CalendarEventStatus.CONFIRMED,
            Instant.parse("2026-04-20T09:00:00Z"),
            3_600_000L,
            true,
            Instant.parse("2026-04-20T10:00:00Z"),
            Instant.parse("2026-04-18T09:00:00Z"),
            Instant.parse("2026-04-19T09:00:00Z"),
            new RRuleOptions(
                RRuleFrequency.DAILY,
                1,
                3,
                Instant.parse("2026-04-22T09:00:00Z"),
                null,
                null,
                List.of(20),
                List.of(Month.APRIL),
                null,
                null,
                "mo"),
            List.of(Instant.parse("2026-04-21T09:00:00Z")),
            Instant.parse("2026-04-22T09:00:00Z"),
            recurringEventId,
            Instant.parse("2026-04-20T09:00:00Z"),
            calendarId,
            userId,
            accountId,
            List.of(
                new CalendarEventReminder(3_600_000L, "email"),
                new CalendarEventReminder(900_000L, "push")),
            serviceId,
            Map.of("source", "java-test", "nested", Map.of("flag", true)));

    repository.insert(event);

    CalendarEvent persisted = repository.find(eventId).orElseThrow();

    assertEquals(event, persisted);
  }

  @Test
  void saveUpdatesPersistedFields() {
    var accountId = id("00000000-0000-0000-0000-000000000311");
    var userId = id("00000000-0000-0000-0000-000000000312");
    var serviceId = id("00000000-0000-0000-0000-000000000313");
    var calendarId = id("00000000-0000-0000-0000-000000000314");
    var eventId = id("00000000-0000-0000-0000-000000000315");

    insertAccount(accountId);
    insertUser(accountId, userId);
    insertService(accountId, serviceId);
    insertCalendar(accountId, userId, calendarId);

    PostgresEventRepository repository = new PostgresEventRepository(jdbcTemplate, objectMapper);
    CalendarEvent original =
        event(
            eventId,
            calendarId,
            userId,
            accountId,
            serviceId,
            "Initial title",
            Instant.parse("2026-04-20T12:00:00Z"),
            Instant.parse("2026-04-20T13:00:00Z"),
            CalendarEventStatus.CONFIRMED,
            true,
            Map.of("source", "initial"));
    repository.insert(original);

    CalendarEvent updated =
        new CalendarEvent(
            eventId,
            "updated-parent",
            "updated-external",
            "Updated title",
            "Updated description",
            "manual",
            "Remote",
            false,
            CalendarEventStatus.CANCELLED,
            Instant.parse("2026-04-20T14:00:00Z"),
            5_400_000L,
            false,
            Instant.parse("2026-04-20T15:30:00Z"),
            Instant.parse("2026-04-18T12:00:00Z"),
            Instant.parse("2026-04-20T11:00:00Z"),
            null,
            List.of(),
            null,
            null,
            null,
            calendarId,
            userId,
            accountId,
            List.of(new CalendarEventReminder(600_000L, "slack")),
            serviceId,
            Map.of("source", "updated", "version", 2));

    repository.save(updated);

    assertEquals(updated, repository.find(eventId).orElseThrow());
  }

  @Test
  void findByServiceReturnsOnlyRequestedUsersAndOverlappingEvents() {
    var accountId = id("00000000-0000-0000-0000-000000000321");
    var userId1 = id("00000000-0000-0000-0000-000000000322");
    var userId2 = id("00000000-0000-0000-0000-000000000323");
    var otherUserId = id("00000000-0000-0000-0000-000000000324");
    var serviceId = id("00000000-0000-0000-0000-000000000325");
    var otherServiceId = id("00000000-0000-0000-0000-000000000326");
    var calendarId1 = id("00000000-0000-0000-0000-000000000327");
    var calendarId2 = id("00000000-0000-0000-0000-000000000328");
    var calendarId3 = id("00000000-0000-0000-0000-000000000329");
    var includedEventId = id("00000000-0000-0000-0000-00000000032a");
    var overlappingEventId = id("00000000-0000-0000-0000-00000000032b");
    var wrongUserEventId = id("00000000-0000-0000-0000-00000000032c");
    var wrongServiceEventId = id("00000000-0000-0000-0000-00000000032d");
    var outsideWindowEventId = id("00000000-0000-0000-0000-00000000032e");

    insertAccount(accountId);
    insertUser(accountId, userId1);
    insertUser(accountId, userId2);
    insertUser(accountId, otherUserId);
    insertService(accountId, serviceId);
    insertService(accountId, otherServiceId);
    insertCalendar(accountId, userId1, calendarId1);
    insertCalendar(accountId, userId2, calendarId2);
    insertCalendar(accountId, otherUserId, calendarId3);

    PostgresEventRepository repository = new PostgresEventRepository(jdbcTemplate, objectMapper);
    CalendarEvent included =
        event(
            includedEventId,
            calendarId1,
            userId1,
            accountId,
            serviceId,
            "Included",
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            CalendarEventStatus.CONFIRMED,
            true,
            Map.of("kind", "included"));
    CalendarEvent overlapping =
        event(
            overlappingEventId,
            calendarId2,
            userId2,
            accountId,
            serviceId,
            "Overlapping",
            Instant.parse("2026-04-20T08:30:00Z"),
            Instant.parse("2026-04-20T09:30:00Z"),
            CalendarEventStatus.CONFIRMED,
            true,
            Map.of("kind", "overlap"));
    CalendarEvent wrongUser =
        event(
            wrongUserEventId,
            calendarId3,
            otherUserId,
            accountId,
            serviceId,
            "Wrong user",
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            CalendarEventStatus.CONFIRMED,
            true,
            Map.of("kind", "wrong-user"));
    CalendarEvent wrongService =
        event(
            wrongServiceEventId,
            calendarId1,
            userId1,
            accountId,
            otherServiceId,
            "Wrong service",
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            CalendarEventStatus.CONFIRMED,
            true,
            Map.of("kind", "wrong-service"));
    CalendarEvent outsideWindow =
        event(
            outsideWindowEventId,
            calendarId1,
            userId1,
            accountId,
            serviceId,
            "Outside window",
            Instant.parse("2026-04-20T12:00:00Z"),
            Instant.parse("2026-04-20T13:00:00Z"),
            CalendarEventStatus.CONFIRMED,
            true,
            Map.of("kind", "outside"));

    repository.insertMany(List.of(included, overlapping, wrongUser, wrongService, outsideWindow));

    List<CalendarEvent> found =
        repository.findByService(
            serviceId,
            List.of(userId1, userId2),
            Instant.parse("2026-04-20T08:45:00Z"),
            Instant.parse("2026-04-20T10:15:00Z"));

    assertEquals(2, found.size());
    assertTrue(found.contains(included));
    assertTrue(found.contains(overlapping));
  }

  @Test
  void findByMetadataMatchesJsonbSubset() {
    var accountId = id("00000000-0000-0000-0000-000000000331");
    var userId = id("00000000-0000-0000-0000-000000000332");
    var serviceId = id("00000000-0000-0000-0000-000000000333");
    var calendarId = id("00000000-0000-0000-0000-000000000334");
    var matchingEventId = id("00000000-0000-0000-0000-000000000335");
    var otherEventId = id("00000000-0000-0000-0000-000000000336");

    insertAccount(accountId);
    insertUser(accountId, userId);
    insertService(accountId, serviceId);
    insertCalendar(accountId, userId, calendarId);

    PostgresEventRepository repository = new PostgresEventRepository(jdbcTemplate, objectMapper);
    CalendarEvent matching =
        event(
            matchingEventId,
            calendarId,
            userId,
            accountId,
            serviceId,
            "Matching metadata",
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            CalendarEventStatus.CONFIRMED,
            true,
            Map.of("source", "sync", "env", "test"));
    CalendarEvent other =
        event(
            otherEventId,
            calendarId,
            userId,
            accountId,
            serviceId,
            "Other metadata",
            Instant.parse("2026-04-20T11:00:00Z"),
            Instant.parse("2026-04-20T12:00:00Z"),
            CalendarEventStatus.CONFIRMED,
            true,
            Map.of("source", "manual", "env", "test"));

    repository.insertMany(List.of(matching, other));

    List<CalendarEvent> found =
        repository.findByMetadata(
            new com.meetsmore.nittei.infra.repos.query.MetadataFindQuery(
                Metadata.of("source", "sync"), 0, 10, accountId));

    assertEquals(List.of(matching), found);
  }

  private static CalendarEvent event(
      ID eventId,
      ID calendarId,
      ID userId,
      ID accountId,
      ID serviceId,
      String title,
      Instant startTime,
      Instant endTime,
      CalendarEventStatus status,
      boolean busy,
      Map<String, Object> metadata) {
    return new CalendarEvent(
        eventId,
        null,
        null,
        title,
        null,
        "booking",
        null,
        false,
        status,
        startTime,
        endTime.toEpochMilli() - startTime.toEpochMilli(),
        busy,
        endTime,
        Instant.parse("2026-04-19T00:00:00Z"),
        Instant.parse("2026-04-19T01:00:00Z"),
        null,
        List.of(),
        null,
        null,
        null,
        calendarId,
        userId,
        accountId,
        List.of(),
        serviceId,
        metadata);
  }
}
