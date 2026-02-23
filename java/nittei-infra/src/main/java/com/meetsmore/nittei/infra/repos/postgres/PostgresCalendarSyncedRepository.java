package com.meetsmore.nittei.infra.repos.postgres;

import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.IntegrationProvider;
import com.meetsmore.nittei.domain.SyncedCalendar;
import com.meetsmore.nittei.infra.repos.CalendarSyncedRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public class PostgresCalendarSyncedRepository implements CalendarSyncedRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresCalendarSyncedRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insert(SyncedCalendar calendar) {
        jdbcTemplate.update(
            "INSERT INTO externally_synced_calendars(calendar_uid, user_uid, ext_calendar_id, provider) VALUES (?, ?, ?, ?)",
            toUuid(calendar.calendarId()),
            toUuid(calendar.userId()),
            calendar.extCalendarId(),
            providerToDb(calendar.provider())
        );
    }

    @Override
    public void delete(SyncedCalendar calendar) {
        int affected = jdbcTemplate.update(
            "DELETE FROM externally_synced_calendars WHERE calendar_uid = ? AND user_uid = ? AND ext_calendar_id = ? AND provider = ?",
            toUuid(calendar.calendarId()),
            toUuid(calendar.userId()),
            calendar.extCalendarId(),
            providerToDb(calendar.provider())
        );
        if (affected != 1) {
            throw new IllegalStateException("Synced calendar not found");
        }
    }

    @Override
    public List<SyncedCalendar> findByCalendar(ID calendarId) {
        return jdbcTemplate.query(
            "SELECT calendar_uid, user_uid, ext_calendar_id, provider FROM externally_synced_calendars WHERE calendar_uid = ?",
            (rs, rowNum) -> new SyncedCalendar(
                providerFromDb(rs.getString("provider")),
                toId(rs.getObject("calendar_uid", UUID.class)),
                toId(rs.getObject("user_uid", UUID.class)),
                rs.getString("ext_calendar_id")
            ),
            toUuid(calendarId)
        );
    }

    private static String providerToDb(IntegrationProvider provider) {
        return switch (provider) {
            case GOOGLE -> "google";
            case OUTLOOK -> "outlook";
        };
    }

    private static IntegrationProvider providerFromDb(String provider) {
        return "outlook".equalsIgnoreCase(provider) ? IntegrationProvider.OUTLOOK : IntegrationProvider.GOOGLE;
    }

    private static UUID toUuid(ID id) {
        return UUID.fromString(id.value());
    }

    private static ID toId(UUID id) {
        return new ID(id.toString());
    }
}
