package com.meetsmore.nittei.infra.repos.postgres;

import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.Reminder;
import com.meetsmore.nittei.infra.repos.ReminderRepository;
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

public class PostgresReminderRepository implements ReminderRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresReminderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void bulkInsert(List<Reminder> reminders) {
        if (reminders.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
            "INSERT INTO reminders(event_uid, account_uid, remind_at, version, identifier) VALUES(?, ?, ?, ?, ?)",
            new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Reminder reminder = reminders.get(i);
                    ps.setObject(1, toUuid(reminder.eventId()));
                    ps.setObject(2, toUuid(reminder.accountId()));
                    ps.setTimestamp(3, Timestamp.from(reminder.remindAt()));
                    ps.setLong(4, reminder.version());
                    ps.setString(5, reminder.identifier());
                }

                @Override
                public int getBatchSize() {
                    return reminders.size();
                }
            }
        );
    }

    @Override
    public long initVersion(ID eventId) {
        return jdbcTemplate.query(
            "INSERT INTO event_reminder_versions(event_uid) VALUES (?) RETURNING version",
            (rs, rowNum) -> rs.getLong("version"),
            toUuid(eventId)
        ).stream().findFirst().orElseThrow(() -> new IllegalStateException("Unable to initialize reminder version"));
    }

    @Override
    public long incVersion(ID eventId) {
        return jdbcTemplate.query(
            """
            WITH prev_v AS (
                DELETE FROM event_reminder_versions WHERE event_uid = ? RETURNING version
            )
            INSERT INTO event_reminder_versions(event_uid, version)
            SELECT ?, version + 1 FROM prev_v
            RETURNING version
            """,
            (rs, rowNum) -> rs.getLong("version"),
            toUuid(eventId),
            toUuid(eventId)
        ).stream().findFirst().orElseThrow(() -> new IllegalStateException("Unable to increment reminder version"));
    }

    @Override
    public List<Reminder> deleteAllBefore(Instant before) {
        return jdbcTemplate.query(
            "DELETE FROM reminders WHERE remind_at <= ? RETURNING event_uid, account_uid, remind_at, version, identifier",
            (rs, rowNum) -> new Reminder(
                toId(rs.getObject("account_uid", UUID.class)),
                toId(rs.getObject("event_uid", UUID.class)),
                rs.getLong("version"),
                toInstant(rs, "remind_at"),
                rs.getString("identifier")
            ),
            Timestamp.from(before)
        );
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
