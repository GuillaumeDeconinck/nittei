package com.meetsmore.nittei.infra.repos.postgres;

import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.IntegrationProvider;
import com.meetsmore.nittei.domain.SyncedCalendarEvent;
import com.meetsmore.nittei.infra.repos.EventSyncedRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public class PostgresEventSyncedRepository implements EventSyncedRepository {

  private final JdbcTemplate jdbcTemplate;

  public PostgresEventSyncedRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void insert(SyncedCalendarEvent event) {
    jdbcTemplate.update(
        """
            INSERT INTO externally_synced_calendar_events(
                event_uid, calendar_uid, ext_calendar_id, ext_calendar_event_id, provider
            ) VALUES (?, ?, ?, ?, ?)
            """,
        toUuid(event.eventId()),
        toUuid(event.calendarId()),
        event.extCalendarId(),
        event.extEventId(),
        providerToDb(event.provider()));
  }

  @Override
  public List<SyncedCalendarEvent> findByEvent(ID eventId) {
    return jdbcTemplate.query(
        """
            SELECT e.event_uid, e.calendar_uid, c.user_uid, e.ext_calendar_id, e.ext_calendar_event_id, e.provider
            FROM externally_synced_calendar_events AS e
            INNER JOIN calendars AS c ON c.calendar_uid = e.calendar_uid
            WHERE e.event_uid = ?
            """,
        (rs, rowNum) ->
            new SyncedCalendarEvent(
                toId(rs.getObject("event_uid", UUID.class)),
                toId(rs.getObject("calendar_uid", UUID.class)),
                toId(rs.getObject("user_uid", UUID.class)),
                rs.getString("ext_calendar_event_id"),
                rs.getString("ext_calendar_id"),
                providerFromDb(rs.getString("provider"))),
        toUuid(eventId));
  }

  private static String providerToDb(IntegrationProvider provider) {
    return switch (provider) {
      case GOOGLE -> "google";
      case OUTLOOK -> "outlook";
    };
  }

  private static IntegrationProvider providerFromDb(String provider) {
    return "outlook".equalsIgnoreCase(provider)
        ? IntegrationProvider.OUTLOOK
        : IntegrationProvider.GOOGLE;
  }

  private static UUID toUuid(ID id) {
    return UUID.fromString(id.value());
  }

  private static ID toId(UUID id) {
    return new ID(id.toString());
  }
}
