package com.meetsmore.nittei.infra.repos.postgres;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetsmore.nittei.domain.*;
import com.meetsmore.nittei.infra.repos.EventRepository;
import com.meetsmore.nittei.infra.repos.query.*;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

public class PostgresEventRepository implements EventRepository {

  private static final String EVENT_COLUMNS =
      "event_uid, calendar_uid, user_uid, account_uid, external_parent_id, external_id, title, description, event_type, location, all_day, status, start_time, duration, busy, end_time, created, updated, recurrence_jsonb::text AS recurrence_jsonb, recurring_until, exdates, recurring_event_uid, original_start_time, reminders_jsonb::text AS reminders_jsonb, service_uid, metadata::text AS metadata";
  private static final String EVENT_BASE_SELECT =
      "SELECT " + EVENT_COLUMNS + " FROM calendar_events AS e";

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public PostgresEventRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  @Override
  public void insert(CalendarEvent event) {
    jdbcTemplate.update(
        """
            INSERT INTO calendar_events(
                event_uid, account_uid, user_uid, calendar_uid, external_parent_id, external_id, title, description, event_type, location,
                status, all_day, start_time, duration, end_time, busy, created, updated, recurrence_jsonb, recurring_until, exdates,
                recurring_event_uid, original_start_time, reminders_jsonb, service_uid, metadata
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, CAST(? AS jsonb), ?, CAST(? AS jsonb))
            """,
        toUuid(event.id()),
        toUuid(event.accountId()),
        toUuid(event.userId()),
        toUuid(event.calendarId()),
        event.externalParentId(),
        event.externalId(),
        event.title(),
        event.description(),
        event.eventType(),
        event.location(),
        statusToDb(event.status()),
        event.allDay(),
        Timestamp.from(event.startTime()),
        Objects.requireNonNullElse(event.duration(), 0L),
        Timestamp.from(event.endTime()),
        event.busy(),
        event.created().toEpochMilli(),
        event.updated().toEpochMilli(),
        toJsonNullable(event.recurrence()),
        toTimestamp(event.recurringUntil()),
        toTimestampArray(event.exdates()),
        event.recurringEventId() == null ? null : toUuid(event.recurringEventId()),
        toTimestamp(event.originalStartTime()),
        toJson(event.reminders()),
        event.serviceId() == null ? null : toUuid(event.serviceId()),
        toJson(event.metadata()));
  }

  @Override
  public void insertMany(List<CalendarEvent> events) {
    if (events.isEmpty()) {
      return;
    }
    jdbcTemplate.batchUpdate(
        """
            INSERT INTO calendar_events(
                event_uid, account_uid, user_uid, calendar_uid, external_parent_id, external_id, title, description, event_type, location,
                status, all_day, start_time, duration, end_time, busy, created, updated, recurrence_jsonb, recurring_until, exdates,
                recurring_event_uid, original_start_time, reminders_jsonb, service_uid, metadata
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, CAST(? AS jsonb), ?, CAST(? AS jsonb))
            """,
        new BatchPreparedStatementSetter() {
          @Override
          public void setValues(PreparedStatement ps, int i) throws SQLException {
            CalendarEvent event = events.get(i);
            ps.setObject(1, toUuid(event.id()));
            ps.setObject(2, toUuid(event.accountId()));
            ps.setObject(3, toUuid(event.userId()));
            ps.setObject(4, toUuid(event.calendarId()));
            ps.setString(5, event.externalParentId());
            ps.setString(6, event.externalId());
            ps.setString(7, event.title());
            ps.setString(8, event.description());
            ps.setString(9, event.eventType());
            ps.setString(10, event.location());
            ps.setString(11, statusToDb(event.status()));
            ps.setBoolean(12, event.allDay());
            ps.setTimestamp(13, Timestamp.from(event.startTime()));
            ps.setLong(14, Objects.requireNonNullElse(event.duration(), 0L));
            ps.setTimestamp(15, Timestamp.from(event.endTime()));
            ps.setBoolean(16, event.busy());
            ps.setLong(17, event.created().toEpochMilli());
            ps.setLong(18, event.updated().toEpochMilli());
            ps.setString(19, toJsonNullable(event.recurrence()));
            ps.setTimestamp(20, toTimestamp(event.recurringUntil()));
            ps.setArray(21, toTimestampArray(event.exdates()));
            if (event.recurringEventId() == null) {
              ps.setObject(22, null);
            } else {
              ps.setObject(22, toUuid(event.recurringEventId()));
            }
            ps.setTimestamp(23, toTimestamp(event.originalStartTime()));
            ps.setString(24, toJson(event.reminders()));
            if (event.serviceId() == null) {
              ps.setObject(25, null);
            } else {
              ps.setObject(25, toUuid(event.serviceId()));
            }
            ps.setString(26, toJson(event.metadata()));
          }

          @Override
          public int getBatchSize() {
            return events.size();
          }
        });
  }

  @Override
  public void save(CalendarEvent event) {
    jdbcTemplate.update(
        """
            UPDATE calendar_events SET
                external_parent_id = ?, external_id = ?, title = ?, description = ?, event_type = ?, location = ?,
                status = ?, all_day = ?, start_time = ?, duration = ?, end_time = ?, busy = ?, created = ?, updated = ?,
                recurrence_jsonb = CAST(? AS jsonb), recurring_until = ?, exdates = ?, recurring_event_uid = ?, original_start_time = ?,
                reminders_jsonb = CAST(? AS jsonb), service_uid = ?, metadata = CAST(? AS jsonb)
            WHERE event_uid = ?
            """,
        event.externalParentId(),
        event.externalId(),
        event.title(),
        event.description(),
        event.eventType(),
        event.location(),
        statusToDb(event.status()),
        event.allDay(),
        Timestamp.from(event.startTime()),
        Objects.requireNonNullElse(event.duration(), 0L),
        Timestamp.from(event.endTime()),
        event.busy(),
        event.created().toEpochMilli(),
        event.updated().toEpochMilli(),
        toJsonNullable(event.recurrence()),
        toTimestamp(event.recurringUntil()),
        toTimestampArray(event.exdates()),
        event.recurringEventId() == null ? null : toUuid(event.recurringEventId()),
        toTimestamp(event.originalStartTime()),
        toJson(event.reminders()),
        event.serviceId() == null ? null : toUuid(event.serviceId()),
        toJson(event.metadata()),
        toUuid(event.id()));
  }

  @Override
  public Optional<CalendarEvent> find(ID eventId) {
    List<CalendarEvent> rows =
        jdbcTemplate.query(
            EVENT_BASE_SELECT + " WHERE e.event_uid = ?",
            (rs, rowNum) -> mapEvent(rs),
            toUuid(eventId));
    return rows.stream().findFirst();
  }

  @Override
  public List<CalendarEvent> findByIdAndRecurringEventId(ID eventId) {
    return jdbcTemplate.query(
        EVENT_BASE_SELECT + " WHERE e.event_uid = ? OR e.recurring_event_uid = ?",
        (rs, rowNum) -> mapEvent(rs),
        toUuid(eventId),
        toUuid(eventId));
  }

  @Override
  public List<CalendarEvent> findByRecurringEventIdsForTimespan(
      List<ID> recurringEventIds, TimeSpan timespan) {
    if (recurringEventIds.isEmpty()) {
      return List.of();
    }
    String sql =
        EVENT_BASE_SELECT
            + " WHERE e.recurring_event_uid IN ("
            + placeholders(recurringEventIds.size())
            + ")"
            + " AND ((e.original_start_time >= ? AND e.original_start_time <= ?) OR (e.start_time <= ? AND e.end_time >= ?))";
    List<Object> args = new ArrayList<>();
    args.addAll(toUuidList(recurringEventIds));
    args.add(Timestamp.from(timespan.start()));
    args.add(Timestamp.from(timespan.end()));
    args.add(Timestamp.from(timespan.end()));
    args.add(Timestamp.from(timespan.start()));
    return jdbcTemplate.query(sql, (rs, rowNum) -> mapEvent(rs), args.toArray());
  }

  @Override
  public List<CalendarEvent> getByExternalId(ID accountId, String externalId) {
    return jdbcTemplate.query(
        EVENT_BASE_SELECT + " WHERE e.account_uid = ? AND e.external_id = ?",
        (rs, rowNum) -> mapEvent(rs),
        toUuid(accountId),
        externalId);
  }

  @Override
  public List<CalendarEvent> findManyByExternalIds(ID accountId, List<String> externalIds) {
    if (externalIds.isEmpty()) {
      return List.of();
    }
    String sql =
        EVENT_BASE_SELECT
            + " WHERE e.account_uid = ? AND e.external_id IN ("
            + placeholders(externalIds.size())
            + ")";
    List<Object> args = new ArrayList<>();
    args.add(toUuid(accountId));
    args.addAll(externalIds);
    return jdbcTemplate.query(sql, (rs, rowNum) -> mapEvent(rs), args.toArray());
  }

  @Override
  public List<CalendarEvent> findMany(List<ID> eventIds) {
    if (eventIds.isEmpty()) {
      return List.of();
    }
    String sql =
        EVENT_BASE_SELECT + " WHERE e.event_uid IN (" + placeholders(eventIds.size()) + ")";
    return jdbcTemplate.query(sql, (rs, rowNum) -> mapEvent(rs), toUuidList(eventIds).toArray());
  }

  @Override
  public List<CalendarEvent> findByCalendar(ID calendarId, TimeSpan timespan) {
    return jdbcTemplate.query(
        EVENT_BASE_SELECT
            + " WHERE e.calendar_uid = ? AND ((e.start_time <= ? AND e.end_time >= ?) OR (e.start_time < ? AND e.recurrence_jsonb IS NOT NULL AND (e.recurring_until IS NULL OR e.recurring_until > ?)))",
        (rs, rowNum) -> mapEvent(rs),
        toUuid(calendarId),
        Timestamp.from(timespan.end()),
        Timestamp.from(timespan.start()),
        Timestamp.from(timespan.end()),
        Timestamp.from(timespan.start()));
  }

  @Override
  public List<CalendarEvent> findEventsForUsersForTimespan(
      List<ID> userIds, TimeSpan timespan, boolean includeTentative, boolean includeNonBusy) {
    if (userIds.isEmpty()) {
      return List.of();
    }
    List<Boolean> busyValues = includeNonBusy ? List.of(true, false) : List.of(true);
    List<String> statuses = statusesForQuery(includeTentative);
    String sql =
        EVENT_BASE_SELECT
            + " WHERE e.user_uid IN ("
            + placeholders(userIds.size())
            + ")"
            + " AND e.start_time <= ? AND e.end_time >= ?"
            + " AND e.busy IN ("
            + placeholders(busyValues.size())
            + ")"
            + " AND e.status IN ("
            + placeholders(statuses.size())
            + ")"
            + " AND e.recurrence_jsonb IS NULL AND e.original_start_time IS NULL";
    List<Object> args = new ArrayList<>();
    args.addAll(toUuidList(userIds));
    args.add(Timestamp.from(timespan.end()));
    args.add(Timestamp.from(timespan.start()));
    args.addAll(busyValues);
    args.addAll(statuses);
    return jdbcTemplate.query(sql, (rs, rowNum) -> mapEvent(rs), args.toArray());
  }

  @Override
  public List<CalendarEvent> findRecurringEventsForUsersForTimespan(
      List<ID> userIds, TimeSpan timespan, boolean includeTentative, boolean includeNonBusy) {
    if (userIds.isEmpty()) {
      return List.of();
    }
    List<Boolean> busyValues = includeNonBusy ? List.of(true, false) : List.of(true);
    List<String> statuses = statusesForQuery(includeTentative);
    String sql =
        EVENT_BASE_SELECT
            + " WHERE e.user_uid IN ("
            + placeholders(userIds.size())
            + ")"
            + " AND e.start_time <= ? AND e.recurrence_jsonb IS NOT NULL AND (e.recurring_until IS NULL OR e.recurring_until >= ?)"
            + " AND e.busy IN ("
            + placeholders(busyValues.size())
            + ")"
            + " AND e.status IN ("
            + placeholders(statuses.size())
            + ")";
    List<Object> args = new ArrayList<>();
    args.addAll(toUuidList(userIds));
    args.add(Timestamp.from(timespan.end()));
    args.add(Timestamp.from(timespan.start()));
    args.addAll(busyValues);
    args.addAll(statuses);
    return jdbcTemplate.query(sql, (rs, rowNum) -> mapEvent(rs), args.toArray());
  }

  @Override
  public List<CalendarEvent> findByCalendars(List<ID> calendarIds, TimeSpan timespan) {
    if (calendarIds.isEmpty()) {
      return List.of();
    }
    String sql =
        EVENT_BASE_SELECT
            + " WHERE e.calendar_uid IN ("
            + placeholders(calendarIds.size())
            + ")"
            + " AND ((e.start_time <= ? AND e.end_time >= ?) OR (e.start_time < ? AND e.recurrence_jsonb IS NOT NULL AND (e.recurring_until IS NULL OR e.recurring_until > ?)))";
    List<Object> args = new ArrayList<>();
    args.addAll(toUuidList(calendarIds));
    args.add(Timestamp.from(timespan.end()));
    args.add(Timestamp.from(timespan.start()));
    args.add(Timestamp.from(timespan.end()));
    args.add(Timestamp.from(timespan.start()));
    return jdbcTemplate.query(sql, (rs, rowNum) -> mapEvent(rs), args.toArray());
  }

  @Override
  public List<CalendarEvent> findBusyEventsAndRecurringEventsForCalendars(
      List<ID> calendarIds, TimeSpan timespan, boolean includeTentative) {
    if (calendarIds.isEmpty()) {
      return List.of();
    }
    List<String> statuses = statusesForQuery(includeTentative);
    String sql =
        EVENT_BASE_SELECT
            + " WHERE e.calendar_uid IN ("
            + placeholders(calendarIds.size())
            + ")"
            + " AND ((e.start_time < ? AND e.end_time > ?) OR (e.start_time < ? AND e.recurrence_jsonb IS NOT NULL AND (e.recurring_until IS NULL OR e.recurring_until > ?)))"
            + " AND e.busy = true AND e.status IN ("
            + placeholders(statuses.size())
            + ")";
    List<Object> args = new ArrayList<>();
    args.addAll(toUuidList(calendarIds));
    args.add(Timestamp.from(timespan.end()));
    args.add(Timestamp.from(timespan.start()));
    args.add(Timestamp.from(timespan.end()));
    args.add(Timestamp.from(timespan.start()));
    args.addAll(statuses);
    return jdbcTemplate.query(sql, (rs, rowNum) -> mapEvent(rs), args.toArray());
  }

  @Override
  public List<CalendarEvent> searchEventsForUser(SearchEventsForUserParams params) {
    StringBuilder sql = new StringBuilder(EVENT_BASE_SELECT + " WHERE e.user_uid = ?");
    List<Object> args = new ArrayList<>();
    args.add(toUuid(params.userId()));

    SearchEventsParams search = params.searchEventsParams();
    applyIdQuery(sql, args, "event_uid", search.eventUid());
    if (params.calendarIds() != null && !params.calendarIds().isEmpty()) {
      sql.append(" AND e.calendar_uid IN (")
          .append(placeholders(params.calendarIds().size()))
          .append(")");
      args.addAll(toUuidList(params.calendarIds()));
    }
    applyStringQuery(sql, args, "external_id", search.externalId());
    applyStringQuery(sql, args, "external_parent_id", search.externalParentId());
    applyDateTimeQuery(sql, args, "start_time", search.startTime(), false);
    applyDateTimeQuery(sql, args, "end_time", search.endTime(), false);
    applyStringQuery(sql, args, "event_type", search.eventType());
    applyStringQuery(sql, args, "status", search.status());
    applyIdQuery(sql, args, "recurring_event_uid", search.recurringEventUid());
    applyDateTimeQuery(sql, args, "original_start_time", search.originalStartTime(), false);
    applyRecurrenceQuery(sql, args, search.recurrence());
    applyMetadataQuery(sql, args, search.metadata());
    applyDateTimeQuery(sql, args, "created", search.createdAt(), true);
    applyDateTimeQuery(sql, args, "updated", search.updatedAt(), true);
    applySortAndLimit(sql, args, params.sort(), params.limit());

    return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapEvent(rs), args.toArray());
  }

  @Override
  public List<CalendarEvent> searchEventsForAccount(SearchEventsForAccountParams params) {
    StringBuilder sql = new StringBuilder(EVENT_BASE_SELECT + " WHERE e.account_uid = ?");
    List<Object> args = new ArrayList<>();
    args.add(toUuid(params.accountId()));

    SearchEventsParams search = params.searchEventsParams();
    applyIdQuery(sql, args, "event_uid", search.eventUid());
    applyIdQuery(sql, args, "user_uid", search.userUid());
    applyStringQuery(sql, args, "external_id", search.externalId());
    applyStringQuery(sql, args, "external_parent_id", search.externalParentId());
    applyDateTimeQuery(sql, args, "start_time", search.startTime(), false);
    applyDateTimeQuery(sql, args, "end_time", search.endTime(), false);
    applyStringQuery(sql, args, "event_type", search.eventType());
    applyStringQuery(sql, args, "status", search.status());
    applyIdQuery(sql, args, "recurring_event_uid", search.recurringEventUid());
    applyDateTimeQuery(sql, args, "original_start_time", search.originalStartTime(), false);
    applyRecurrenceQuery(sql, args, search.recurrence());
    applyMetadataQuery(sql, args, search.metadata());
    applyDateTimeQuery(sql, args, "created", search.createdAt(), true);
    applyDateTimeQuery(sql, args, "updated", search.updatedAt(), true);
    applySortAndLimit(sql, args, params.sort(), params.limit());

    return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapEvent(rs), args.toArray());
  }

  @Override
  public List<MostRecentCreatedServiceEvents> findMostRecentlyCreatedServiceEvents(
      ID serviceId, List<ID> userIds) {
    if (userIds.isEmpty()) {
      return List.of();
    }
    String sql =
        """
            SELECT users.user_uid, events.created FROM users
            LEFT JOIN (
                SELECT DISTINCT ON (e.user_uid) e.user_uid, e.created
                FROM calendar_events AS e
                WHERE service_uid = ?
                ORDER BY e.user_uid, created DESC
            ) AS events ON events.user_uid = users.user_uid
            WHERE users.user_uid IN (__USER_ID_PLACEHOLDERS__)
            """
            .replace("__USER_ID_PLACEHOLDERS__", placeholders(userIds.size()));
    List<Object> args = new ArrayList<>();
    args.add(toUuid(serviceId));
    args.addAll(toUuidList(userIds));
    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          Long created = rs.getObject("created", Long.class);
          return new MostRecentCreatedServiceEvents(
              toId(rs.getObject("user_uid", UUID.class)),
              created == null ? null : Instant.ofEpochMilli(created));
        },
        args.toArray());
  }

  @Override
  public List<CalendarEvent> findByService(
      ID serviceId, List<ID> userIds, Instant minTime, Instant maxTime) {
    if (userIds.isEmpty()) {
      return List.of();
    }
    String sql =
        EVENT_BASE_SELECT
            + " WHERE e.service_uid = ? AND e.user_uid IN ("
            + placeholders(userIds.size())
            + ") AND e.start_time <= ? AND e.end_time >= ?";
    List<Object> args = new ArrayList<>();
    args.add(toUuid(serviceId));
    args.addAll(toUuidList(userIds));
    args.add(Timestamp.from(maxTime));
    args.add(Timestamp.from(minTime));
    return jdbcTemplate.query(sql, (rs, rowNum) -> mapEvent(rs), args.toArray());
  }

  @Override
  public List<CalendarEvent> findUserServiceEvents(
      ID userId, boolean busy, Instant minTime, Instant maxTime) {
    return jdbcTemplate.query(
        EVENT_BASE_SELECT
            + " WHERE e.user_uid = ? AND e.busy = ? AND e.service_uid IS NOT NULL AND e.start_time <= ? AND e.end_time >= ?",
        (rs, rowNum) -> mapEvent(rs),
        toUuid(userId),
        busy,
        Timestamp.from(maxTime),
        Timestamp.from(minTime));
  }

  @Override
  public void delete(ID eventId) {
    jdbcTemplate.update("DELETE FROM calendar_events WHERE event_uid = ?", toUuid(eventId));
  }

  @Override
  public void deleteMany(List<ID> eventIds) {
    if (eventIds.isEmpty()) {
      return;
    }
    String sql =
        "DELETE FROM calendar_events WHERE event_uid IN (" + placeholders(eventIds.size()) + ")";
    jdbcTemplate.update(sql, toUuidList(eventIds).toArray());
  }

  @Override
  public void deleteByService(ID serviceId) {
    jdbcTemplate.update("DELETE FROM calendar_events WHERE service_uid = ?", toUuid(serviceId));
  }

  @Override
  public List<CalendarEvent> findByMetadata(MetadataFindQuery query) {
    return jdbcTemplate.query(
        EVENT_BASE_SELECT
            + " WHERE e.account_uid = ? AND e.metadata @> CAST(? AS jsonb) LIMIT ? OFFSET ?",
        (rs, rowNum) -> mapEvent(rs),
        toUuid(query.accountId()),
        toJson(query.metadata() == null ? null : query.metadata().inner()),
        query.limit(),
        query.skip());
  }

  private CalendarEvent mapEvent(ResultSet rs) throws SQLException {
    try {
      UUID recurringEventId = rs.getObject("recurring_event_uid", UUID.class);
      UUID serviceId = rs.getObject("service_uid", UUID.class);
      String recurrenceRaw = rs.getString("recurrence_jsonb");
      String remindersRaw = rs.getString("reminders_jsonb");
      RRuleOptions recurrence =
          recurrenceRaw == null ? null : objectMapper.readValue(recurrenceRaw, RRuleOptions.class);
      List<CalendarEventReminder> reminders =
          remindersRaw == null
              ? List.of()
              : objectMapper.readValue(
                  remindersRaw,
                  objectMapper
                      .getTypeFactory()
                      .constructCollectionType(List.class, CalendarEventReminder.class));
      Object metadata = objectMapper.readValue(rs.getString("metadata"), Object.class);

      return new CalendarEvent(
          toId(rs.getObject("event_uid", UUID.class)),
          rs.getString("external_parent_id"),
          rs.getString("external_id"),
          rs.getString("title"),
          rs.getString("description"),
          rs.getString("event_type"),
          rs.getString("location"),
          rs.getBoolean("all_day"),
          statusFromDb(rs.getString("status")),
          toInstant(rs, "start_time"),
          rs.getLong("duration"),
          rs.getBoolean("busy"),
          toInstant(rs, "end_time"),
          Instant.ofEpochMilli(rs.getLong("created")),
          Instant.ofEpochMilli(rs.getLong("updated")),
          recurrence,
          toInstants(rs.getArray("exdates")),
          toInstantNullable(rs, "recurring_until"),
          recurringEventId == null ? null : toId(recurringEventId),
          toInstantNullable(rs, "original_start_time"),
          toId(rs.getObject("calendar_uid", UUID.class)),
          toId(rs.getObject("user_uid", UUID.class)),
          toId(rs.getObject("account_uid", UUID.class)),
          reminders,
          serviceId == null ? null : toId(serviceId),
          metadata);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Invalid calendar event JSON", e);
    }
  }

  private void applyIdQuery(StringBuilder sql, List<Object> args, String field, IDQuery query) {
    if (query == null) {
      return;
    }
    if (query.eq() != null) {
      sql.append(" AND e.").append(field).append(" = ?");
      args.add(toUuid(query.eq()));
    }
    if (query.ne() != null) {
      sql.append(" AND e.").append(field).append(" != ?");
      args.add(toUuid(query.ne()));
    }
    if (query.exists() != null) {
      sql.append(" AND e.").append(field).append(query.exists() ? " IS NOT NULL" : " IS NULL");
    }
    if (query.in() != null && !query.in().isEmpty()) {
      sql.append(" AND e.")
          .append(field)
          .append(" IN (")
          .append(placeholders(query.in().size()))
          .append(")");
      args.addAll(toUuidList(query.in()));
    }
    if (query.nin() != null && !query.nin().isEmpty()) {
      sql.append(" AND e.")
          .append(field)
          .append(" NOT IN (")
          .append(placeholders(query.nin().size()))
          .append(")");
      args.addAll(toUuidList(query.nin()));
    }
    if (query.gt() != null) {
      sql.append(" AND e.").append(field).append(" > ?");
      args.add(toUuid(query.gt()));
    }
    if (query.gte() != null) {
      sql.append(" AND e.").append(field).append(" >= ?");
      args.add(toUuid(query.gte()));
    }
    if (query.lt() != null) {
      sql.append(" AND e.").append(field).append(" < ?");
      args.add(toUuid(query.lt()));
    }
    if (query.lte() != null) {
      sql.append(" AND e.").append(field).append(" <= ?");
      args.add(toUuid(query.lte()));
    }
  }

  private void applyStringQuery(
      StringBuilder sql, List<Object> args, String field, StringQuery query) {
    if (query == null) {
      return;
    }
    if (query.eq() != null) {
      sql.append(" AND e.").append(field).append(" = ?");
      args.add(query.eq());
    }
    if (query.ne() != null) {
      sql.append(" AND e.").append(field).append(" != ?");
      args.add(query.ne());
    }
    if (query.exists() != null) {
      sql.append(" AND e.").append(field).append(query.exists() ? " IS NOT NULL" : " IS NULL");
    }
    if (query.in() != null && !query.in().isEmpty()) {
      sql.append(" AND e.")
          .append(field)
          .append(" IN (")
          .append(placeholders(query.in().size()))
          .append(")");
      args.addAll(query.in());
    }
  }

  private void applyDateTimeQuery(
      StringBuilder sql,
      List<Object> args,
      String field,
      DateTimeQuery query,
      boolean convertToMillis) {
    if (query == null) {
      return;
    }
    if (query.eq() != null) {
      sql.append(" AND e.").append(field).append(" = ?");
      args.add(convertToMillis ? query.eq().toEpochMilli() : Timestamp.from(query.eq()));
    }
    if (query.range() != null) {
      DateTimeQueryRange range = query.range();
      if (range.gte() != null) {
        sql.append(" AND e.").append(field).append(" >= ?");
        args.add(convertToMillis ? range.gte().toEpochMilli() : Timestamp.from(range.gte()));
      } else if (range.gt() != null) {
        sql.append(" AND e.").append(field).append(" > ?");
        args.add(convertToMillis ? range.gt().toEpochMilli() : Timestamp.from(range.gt()));
      }
      if (range.lte() != null) {
        sql.append(" AND e.").append(field).append(" <= ?");
        args.add(convertToMillis ? range.lte().toEpochMilli() : Timestamp.from(range.lte()));
      } else if (range.lt() != null) {
        sql.append(" AND e.").append(field).append(" < ?");
        args.add(convertToMillis ? range.lt().toEpochMilli() : Timestamp.from(range.lt()));
      }
    }
  }

  private void applyRecurrenceQuery(
      StringBuilder sql, List<Object> args, RecurrenceQuery recurrence) {
    if (recurrence == null) {
      return;
    }
    if (recurrence.existsAndRecurringAt() != null) {
      sql.append(
          " AND e.recurrence_jsonb IS NOT NULL AND (e.recurring_until IS NULL OR e.recurring_until >= ?)");
      args.add(Timestamp.from(recurrence.existsAndRecurringAt()));
      return;
    }
    if (recurrence.exists() != null) {
      sql.append(
          recurrence.exists()
              ? " AND e.recurrence_jsonb IS NOT NULL"
              : " AND e.recurrence_jsonb IS NULL");
    }
  }

  private void applyMetadataQuery(StringBuilder sql, List<Object> args, Object metadata) {
    if (metadata != null) {
      sql.append(" AND e.metadata @> CAST(? AS jsonb)");
      args.add(toJson(metadata));
    }
  }

  private void applySortAndLimit(
      StringBuilder sql, List<Object> args, CalendarEventSort sort, Integer limit) {
    if (sort != null) {
      sql.append(" ORDER BY ")
          .append(
              switch (sort) {
                case START_TIME_ASC -> "start_time ASC";
                case START_TIME_DESC -> "start_time DESC";
                case END_TIME_ASC -> "end_time ASC";
                case END_TIME_DESC -> "end_time DESC";
                case CREATED_ASC -> "created ASC";
                case CREATED_DESC -> "created DESC";
                case UPDATED_ASC -> "updated ASC";
                case UPDATED_DESC -> "updated DESC";
                case EVENT_UID_ASC -> "event_uid ASC";
                case EVENT_UID_DESC -> "event_uid DESC";
              });
    }
    if (limit != null) {
      sql.append(" LIMIT ?");
      args.add(limit);
    }
  }

  private List<String> statusesForQuery(boolean includeTentative) {
    if (includeTentative) {
      return List.of(
          statusToDb(CalendarEventStatus.TENTATIVE), statusToDb(CalendarEventStatus.CONFIRMED));
    }
    return List.of(statusToDb(CalendarEventStatus.CONFIRMED));
  }

  private List<UUID> toUuidList(List<ID> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    List<UUID> uuids = new ArrayList<>(ids.size());
    for (ID id : ids) {
      uuids.add(toUuid(id));
    }
    return uuids;
  }

  private Timestamp toTimestamp(Instant value) {
    return value == null ? null : Timestamp.from(value);
  }

  private Array toTimestampArray(List<Instant> values) {
    List<Instant> safe = values == null ? List.of() : values;
    Timestamp[] timestamps = safe.stream().map(Timestamp::from).toArray(Timestamp[]::new);
    return jdbcTemplate.getDataSource() == null
        ? null
        : jdbcTemplate.execute((Connection con) -> con.createArrayOf("timestamptz", timestamps));
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

  private Instant toInstantNullable(ResultSet rs, String column) throws SQLException {
    OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
    if (value != null) {
      return value.toInstant();
    }
    Timestamp ts = rs.getTimestamp(column);
    return ts == null ? null : ts.toInstant();
  }

  private List<Instant> toInstants(Array sqlArray) throws SQLException {
    if (sqlArray == null) {
      return List.of();
    }
    Object raw = sqlArray.getArray();
    if (!(raw instanceof Object[] values) || values.length == 0) {
      return List.of();
    }
    List<Instant> result = new ArrayList<>(values.length);
    for (Object value : values) {
      if (value instanceof OffsetDateTime odt) {
        result.add(odt.toInstant());
      } else if (value instanceof Timestamp ts) {
        result.add(ts.toInstant());
      }
    }
    return result;
  }

  private CalendarEventStatus statusFromDb(String status) {
    return switch (status == null ? "" : status.toLowerCase(Locale.ROOT)) {
      case "tentative" -> CalendarEventStatus.TENTATIVE;
      case "cancelled" -> CalendarEventStatus.CANCELLED;
      default -> CalendarEventStatus.CONFIRMED;
    };
  }

  private String statusToDb(CalendarEventStatus status) {
    return switch (status) {
      case TENTATIVE -> "tentative";
      case CONFIRMED -> "confirmed";
      case CANCELLED -> "cancelled";
    };
  }

  private String toJsonNullable(Object value) {
    return value == null ? null : toJson(value);
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
