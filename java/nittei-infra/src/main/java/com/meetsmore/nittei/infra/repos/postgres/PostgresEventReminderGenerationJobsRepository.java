package com.meetsmore.nittei.infra.repos.postgres;

import com.meetsmore.nittei.domain.EventRemindersExpansionJob;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.infra.repos.EventReminderGenerationJobsRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

public class PostgresEventReminderGenerationJobsRepository
    implements EventReminderGenerationJobsRepository {

  private final JdbcTemplate jdbcTemplate;

  public PostgresEventReminderGenerationJobsRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void bulkInsert(List<EventRemindersExpansionJob> jobs) {
    if (jobs.isEmpty()) {
      return;
    }
    jdbcTemplate.batchUpdate(
        "INSERT INTO calendar_event_reminder_generation_jobs(event_uid, timestamp, version) VALUES(?, ?, ?)",
        new BatchPreparedStatementSetter() {
          @Override
          public void setValues(PreparedStatement ps, int i) throws SQLException {
            EventRemindersExpansionJob job = jobs.get(i);
            ps.setObject(1, toUuid(job.eventId()));
            ps.setTimestamp(2, Timestamp.from(job.timestamp()));
            ps.setLong(3, job.version());
          }

          @Override
          public int getBatchSize() {
            return jobs.size();
          }
        });
  }

  @Override
  public List<EventRemindersExpansionJob> deleteAllBefore(Instant before) {
    return jdbcTemplate.query(
        "DELETE FROM calendar_event_reminder_generation_jobs WHERE timestamp <= ? RETURNING event_uid, timestamp, version",
        (rs, rowNum) ->
            new EventRemindersExpansionJob(
                toId(rs.getObject("event_uid", UUID.class)),
                toInstant(rs, "timestamp"),
                rs.getLong("version")),
        Timestamp.from(before));
  }

  private Instant toInstant(ResultSet rs, String column) throws SQLException {
    OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
    if (value != null) {
      return value.toInstant();
    }
    Timestamp ts = rs.getTimestamp(column);
    if (ts == null) {
      throw new IllegalStateException("Missing required timestamp column: " + column);
    }
    return ts.toInstant();
  }

  private static UUID toUuid(ID id) {
    return UUID.fromString(id.value());
  }

  private static ID toId(UUID id) {
    return new ID(id.toString());
  }
}
