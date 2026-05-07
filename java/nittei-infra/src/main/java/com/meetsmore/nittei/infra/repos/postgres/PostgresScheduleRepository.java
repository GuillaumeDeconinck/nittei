package com.meetsmore.nittei.infra.repos.postgres;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.Schedule;
import com.meetsmore.nittei.domain.ScheduleRule;
import com.meetsmore.nittei.infra.repos.ScheduleRepository;
import com.meetsmore.nittei.infra.repos.query.MetadataFindQuery;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public class PostgresScheduleRepository implements ScheduleRepository {

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public PostgresScheduleRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  @Override
  public void insert(Schedule schedule) {
    jdbcTemplate.update(
        "INSERT INTO schedules(schedule_uid, user_uid, rules, timezone, metadata) VALUES (?, ?, CAST(? AS json), ?, CAST(? AS jsonb))",
        toUuid(schedule.id()),
        toUuid(schedule.userId()),
        toJson(schedule.rules()),
        schedule.timezone(),
        toJson(schedule.metadata()));
  }

  @Override
  public void save(Schedule schedule) {
    jdbcTemplate.update(
        "UPDATE schedules SET user_uid = ?, rules = CAST(? AS json), timezone = ?, metadata = CAST(? AS jsonb) WHERE schedule_uid = ?",
        toUuid(schedule.userId()),
        toJson(schedule.rules()),
        schedule.timezone(),
        toJson(schedule.metadata()),
        toUuid(schedule.id()));
  }

  @Override
  public Optional<Schedule> find(ID scheduleId) {
    List<Schedule> rows =
        jdbcTemplate.query(
            "SELECT s.schedule_uid, s.user_uid, s.rules::text AS rules, s.timezone, s.metadata::text AS metadata, u.account_uid FROM schedules s JOIN users u ON u.user_uid = s.user_uid WHERE s.schedule_uid = ?",
            (rs, rowNum) -> mapSchedule(rs),
            toUuid(scheduleId));
    return rows.stream().findFirst();
  }

  @Override
  public List<Schedule> findMany(List<ID> scheduleIds) {
    if (scheduleIds.isEmpty()) {
      return List.of();
    }
    String sql =
        "SELECT s.schedule_uid, s.user_uid, s.rules::text AS rules, s.timezone, s.metadata::text AS metadata, u.account_uid FROM schedules s JOIN users u ON u.user_uid = s.user_uid WHERE s.schedule_uid IN ("
            + placeholders(scheduleIds.size())
            + ")";
    Object[] args = scheduleIds.stream().map(PostgresScheduleRepository::toUuid).toArray();
    return jdbcTemplate.query(sql, (rs, rowNum) -> mapSchedule(rs), args);
  }

  @Override
  public List<Schedule> findByUser(ID userId) {
    return jdbcTemplate.query(
        "SELECT s.schedule_uid, s.user_uid, s.rules::text AS rules, s.timezone, s.metadata::text AS metadata, u.account_uid FROM schedules s JOIN users u ON u.user_uid = s.user_uid WHERE s.user_uid = ?",
        (rs, rowNum) -> mapSchedule(rs),
        toUuid(userId));
  }

  @Override
  public void delete(ID scheduleId) {
    jdbcTemplate.update("DELETE FROM schedules WHERE schedule_uid = ?", toUuid(scheduleId));
  }

  @Override
  public List<Schedule> findByMetadata(MetadataFindQuery query) {
    return jdbcTemplate.query(
        "SELECT s.schedule_uid, s.user_uid, s.rules::text AS rules, s.timezone, s.metadata::text AS metadata, u.account_uid FROM schedules s JOIN users u ON u.user_uid = s.user_uid WHERE u.account_uid = ? AND s.metadata @> CAST(? AS jsonb) LIMIT ? OFFSET ?",
        (rs, rowNum) -> mapSchedule(rs),
        toUuid(query.accountId()),
        toJson(query.metadata() == null ? null : query.metadata().inner()),
        query.limit(),
        query.skip());
  }

  private Schedule mapSchedule(ResultSet rs) throws SQLException {
    try {
      List<ScheduleRule> rules =
          objectMapper.readValue(
              rs.getString("rules"),
              objectMapper
                  .getTypeFactory()
                  .constructCollectionType(List.class, ScheduleRule.class));
      Object metadata = objectMapper.readValue(rs.getString("metadata"), Object.class);
      return new Schedule(
          toId(rs.getObject("schedule_uid", UUID.class)),
          toId(rs.getObject("user_uid", UUID.class)),
          toId(rs.getObject("account_uid", UUID.class)),
          rules,
          rs.getString("timezone"),
          metadata);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Invalid schedule JSON", e);
    }
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
