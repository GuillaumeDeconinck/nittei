package com.meetsmore.nittei.infra.repos.postgres;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetsmore.nittei.domain.Calendar;
import com.meetsmore.nittei.domain.CalendarSettings;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.infra.repos.CalendarRepository;
import com.meetsmore.nittei.infra.repos.query.MetadataFindQuery;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public class PostgresCalendarRepository implements CalendarRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PostgresCalendarRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void insert(Calendar calendar) {
        jdbcTemplate.update(
            "INSERT INTO calendars(calendar_uid, user_uid, account_uid, name, key, settings, metadata) VALUES (?, ?, ?, ?, ?, CAST(? AS json), CAST(? AS jsonb))",
            toUuid(calendar.id()),
            toUuid(calendar.userId()),
            toUuid(calendar.accountId()),
            calendar.name(),
            calendar.key(),
            toJson(Map.of(
                "weekStart", calendar.settings().weekStart().name().substring(0, 3),
                "timezone", calendar.settings().timezone()
            )),
            toJson(calendar.metadata())
        );
    }

    @Override
    public void save(Calendar calendar) {
        jdbcTemplate.update(
            "UPDATE calendars SET user_uid = ?, account_uid = ?, name = ?, key = ?, settings = CAST(? AS json), metadata = CAST(? AS jsonb) WHERE calendar_uid = ?",
            toUuid(calendar.userId()),
            toUuid(calendar.accountId()),
            calendar.name(),
            calendar.key(),
            toJson(Map.of(
                "weekStart", calendar.settings().weekStart().name().substring(0, 3),
                "timezone", calendar.settings().timezone()
            )),
            toJson(calendar.metadata()),
            toUuid(calendar.id())
        );
    }

    @Override
    public Optional<Calendar> find(ID calendarId) {
        List<Calendar> rows = jdbcTemplate.query(
            "SELECT calendar_uid, user_uid, account_uid, name, key, settings::text AS settings, metadata::text AS metadata FROM calendars WHERE calendar_uid = ?",
            (rs, rowNum) -> mapCalendar(rs),
            toUuid(calendarId)
        );
        return rows.stream().findFirst();
    }

    @Override
    public List<Calendar> findMultiple(List<ID> calendarIds) {
        if (calendarIds.isEmpty()) {
            return List.of();
        }
        String sql = "SELECT calendar_uid, user_uid, account_uid, name, key, settings::text AS settings, metadata::text AS metadata FROM calendars WHERE calendar_uid IN ("
            + placeholders(calendarIds.size()) + ")";
        Object[] args = calendarIds.stream().map(PostgresCalendarRepository::toUuid).toArray();
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapCalendar(rs), args);
    }

    @Override
    public List<Calendar> findByUser(ID userId) {
        return jdbcTemplate.query(
            "SELECT calendar_uid, user_uid, account_uid, name, key, settings::text AS settings, metadata::text AS metadata FROM calendars WHERE user_uid = ?",
            (rs, rowNum) -> mapCalendar(rs),
            toUuid(userId)
        );
    }

    @Override
    public Optional<Calendar> findByUserAndKey(ID userId, String key) {
        List<Calendar> rows = jdbcTemplate.query(
            "SELECT calendar_uid, user_uid, account_uid, name, key, settings::text AS settings, metadata::text AS metadata FROM calendars WHERE user_uid = ? AND key = ?",
            (rs, rowNum) -> mapCalendar(rs),
            toUuid(userId),
            key
        );
        return rows.stream().findFirst();
    }

    @Override
    public List<Calendar> findForUsers(List<ID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        String sql = "SELECT calendar_uid, user_uid, account_uid, name, key, settings::text AS settings, metadata::text AS metadata FROM calendars WHERE user_uid IN ("
            + placeholders(userIds.size()) + ")";
        Object[] args = userIds.stream().map(PostgresCalendarRepository::toUuid).toArray();
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapCalendar(rs), args);
    }

    @Override
    public void delete(ID calendarId) {
        jdbcTemplate.update("DELETE FROM calendars WHERE calendar_uid = ?", toUuid(calendarId));
    }

    @Override
    public List<Calendar> findByMetadata(MetadataFindQuery query) {
        return jdbcTemplate.query(
            "SELECT calendar_uid, user_uid, account_uid, name, key, settings::text AS settings, metadata::text AS metadata FROM calendars WHERE account_uid = ? AND metadata @> CAST(? AS jsonb) LIMIT ? OFFSET ?",
            (rs, rowNum) -> mapCalendar(rs),
            toUuid(query.accountId()),
            toJson(query.metadata() == null ? null : query.metadata().inner()),
            query.limit(),
            query.skip()
        );
    }

    private Calendar mapCalendar(ResultSet rs) throws SQLException {
        try {
            Map<String, Object> settings = objectMapper.readValue(rs.getString("settings"), Map.class);
            String weekStartRaw = String.valueOf(settings.getOrDefault("weekStart", "Mon"));
            DayOfWeek weekStart = weekdayFromAny(weekStartRaw);
            String timezone = String.valueOf(settings.getOrDefault("timezone", "UTC"));
            CalendarSettings calendarSettings = new CalendarSettings(weekStart, timezone);
            Object metadata = objectMapper.readValue(rs.getString("metadata"), Object.class);

            return new Calendar(
                toId(rs.getObject("calendar_uid", UUID.class)),
                toId(rs.getObject("user_uid", UUID.class)),
                toId(rs.getObject("account_uid", UUID.class)),
                rs.getString("name"),
                rs.getString("key"),
                calendarSettings,
                metadata
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid calendar JSON", e);
        }
    }

    private DayOfWeek weekdayFromAny(String raw) {
        String n = raw.trim().toLowerCase(Locale.ROOT);
        return switch (n) {
            case "mon", "monday" -> DayOfWeek.MONDAY;
            case "tue", "tuesday" -> DayOfWeek.TUESDAY;
            case "wed", "wednesday" -> DayOfWeek.WEDNESDAY;
            case "thu", "thursday" -> DayOfWeek.THURSDAY;
            case "fri", "friday" -> DayOfWeek.FRIDAY;
            case "sat", "saturday" -> DayOfWeek.SATURDAY;
            case "sun", "sunday" -> DayOfWeek.SUNDAY;
            default -> DayOfWeek.MONDAY;
        };
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }

    private static String placeholders(int count) {
        List<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values.add("?");
        }
        return String.join(",", values);
    }

    private static UUID toUuid(ID id) {
        return UUID.fromString(id.value());
    }

    private static ID toId(UUID id) {
        return new ID(id.toString());
    }
}
