package com.meetsmore.nittei.infra.repos.postgres;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.Service;
import com.meetsmore.nittei.domain.ServiceMultiPersonOptions;
import com.meetsmore.nittei.domain.ServiceResource;
import com.meetsmore.nittei.domain.ServiceWithUsers;
import com.meetsmore.nittei.domain.TimePlan;
import com.meetsmore.nittei.infra.repos.ServiceRepository;
import com.meetsmore.nittei.infra.repos.query.MetadataFindQuery;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public class PostgresServiceRepository implements ServiceRepository {

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public PostgresServiceRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  @Override
  public void insert(Service service) {
    jdbcTemplate.update(
        "INSERT INTO services(service_uid, account_uid, multi_person, metadata) VALUES (?, ?, CAST(? AS json), CAST(? AS jsonb))",
        toUuid(service.id()),
        toUuid(service.accountId()),
        toJson(service.multiPerson()),
        toJson(service.metadata()));
  }

  @Override
  public void save(Service service) {
    jdbcTemplate.update(
        "UPDATE services SET account_uid = ?, multi_person = CAST(? AS json), metadata = CAST(? AS jsonb) WHERE service_uid = ?",
        toUuid(service.accountId()),
        toJson(service.multiPerson()),
        toJson(service.metadata()),
        toUuid(service.id()));
  }

  @Override
  public Optional<Service> find(ID serviceId) {
    List<Service> rows =
        jdbcTemplate.query(
            "SELECT service_uid, account_uid, multi_person::text AS multi_person, metadata::text AS metadata FROM services WHERE service_uid = ?",
            (rs, rowNum) -> mapService(rs),
            toUuid(serviceId));
    return rows.stream().findFirst();
  }

  @Override
  public Optional<ServiceWithUsers> findWithUsers(ID serviceId) {
    Optional<Service> service = find(serviceId);
    if (service.isEmpty()) {
      return Optional.empty();
    }

    List<ServiceResource> users =
        jdbcTemplate.query(
            "SELECT service_uid, user_uid, available_calendar_uid, available_schedule_uid, buffer_after, buffer_before, closest_booking_time, furthest_booking_time FROM service_users WHERE service_uid = ?",
            (rs, rowNum) -> {
              UUID calendarId = rs.getObject("available_calendar_uid", UUID.class);
              UUID scheduleId = rs.getObject("available_schedule_uid", UUID.class);
              TimePlan plan;
              if (calendarId != null) {
                plan = new TimePlan("Calendar", toId(calendarId));
              } else if (scheduleId != null) {
                plan = new TimePlan("Schedule", toId(scheduleId));
              } else {
                plan = new TimePlan("Empty", null);
              }

              return new ServiceResource(
                  toId(rs.getObject("user_uid", UUID.class)),
                  toId(rs.getObject("service_uid", UUID.class)),
                  plan,
                  rs.getLong("buffer_after"),
                  rs.getLong("buffer_before"),
                  rs.getLong("closest_booking_time"),
                  rs.getObject("furthest_booking_time") == null
                      ? null
                      : rs.getLong("furthest_booking_time"));
            },
            toUuid(serviceId));

    Service s = service.get();
    return Optional.of(
        new ServiceWithUsers(s.id(), s.accountId(), users, s.multiPerson(), s.metadata()));
  }

  @Override
  public void delete(ID serviceId) {
    jdbcTemplate.update("DELETE FROM services WHERE service_uid = ?", toUuid(serviceId));
  }

  @Override
  public List<Service> findByMetadata(MetadataFindQuery query) {
    return jdbcTemplate.query(
        "SELECT service_uid, account_uid, multi_person::text AS multi_person, metadata::text AS metadata FROM services WHERE account_uid = ? AND metadata @> CAST(? AS jsonb) LIMIT ? OFFSET ?",
        (rs, rowNum) -> mapService(rs),
        toUuid(query.accountId()),
        toJson(query.metadata() == null ? null : query.metadata().inner()),
        query.limit(),
        query.skip());
  }

  private Service mapService(ResultSet rs) throws SQLException {
    try {
      Map<String, Object> mp = objectMapper.readValue(rs.getString("multi_person"), Map.class);
      if (mp == null) {
        mp = Map.of();
      }
      ServiceMultiPersonOptions multiPerson =
          new ServiceMultiPersonOptions(
              String.valueOf(mp.getOrDefault("variant", "collective")), mp.get("data"));
      Object metadata = objectMapper.readValue(rs.getString("metadata"), Object.class);
      return new Service(
          toId(rs.getObject("service_uid", UUID.class)),
          toId(rs.getObject("account_uid", UUID.class)),
          multiPerson,
          metadata);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Invalid service JSON", e);
    }
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("JSON serialization failed", e);
    }
  }

  private static UUID toUuid(ID id) {
    return UUID.fromString(id.value());
  }

  private static ID toId(UUID id) {
    return new ID(id.toString());
  }
}
