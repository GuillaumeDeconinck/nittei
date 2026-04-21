package com.meetsmore.nittei.infra.repos.postgres;

import com.meetsmore.nittei.domain.BusyCalendarProvider;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.IntegrationProvider;
import com.meetsmore.nittei.infra.repos.BusyCalendarIdentifier;
import com.meetsmore.nittei.infra.repos.ExternalBusyCalendarIdentifier;
import com.meetsmore.nittei.infra.repos.ServiceUserBusyCalendarRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public class PostgresServiceUserBusyCalendarRepository
    implements ServiceUserBusyCalendarRepository {

  private final JdbcTemplate jdbcTemplate;

  public PostgresServiceUserBusyCalendarRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public boolean exists(BusyCalendarIdentifier input) {
    Integer c =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM service_user_busy_calendars WHERE service_uid = ? AND user_uid = ? AND calendar_uid = ?",
            Integer.class,
            toUuid(input.serviceId()),
            toUuid(input.userId()),
            toUuid(input.calendarId()));
    return c != null && c == 1;
  }

  @Override
  public boolean existsExternal(ExternalBusyCalendarIdentifier input) {
    Integer c =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM service_user_external_busy_calendars WHERE service_uid = ? AND user_uid = ? AND ext_calendar_id = ?",
            Integer.class,
            toUuid(input.serviceId()),
            toUuid(input.userId()),
            input.extCalendarId());
    return c != null && c == 1;
  }

  @Override
  public void insert(BusyCalendarIdentifier input) {
    jdbcTemplate.update(
        "INSERT INTO service_user_busy_calendars(service_uid, user_uid, calendar_uid) VALUES (?, ?, ?)",
        toUuid(input.serviceId()),
        toUuid(input.userId()),
        toUuid(input.calendarId()));
  }

  @Override
  public void insertExternal(ExternalBusyCalendarIdentifier input) {
    jdbcTemplate.update(
        "INSERT INTO service_user_external_busy_calendars(service_uid, user_uid, ext_calendar_id, provider) VALUES (?, ?, ?, ?)",
        toUuid(input.serviceId()),
        toUuid(input.userId()),
        input.extCalendarId(),
        providerToDb(input.provider()));
  }

  @Override
  public void delete(BusyCalendarIdentifier input) {
    jdbcTemplate.update(
        "DELETE FROM service_user_busy_calendars WHERE service_uid = ? AND user_uid = ? AND calendar_uid = ?",
        toUuid(input.serviceId()),
        toUuid(input.userId()),
        toUuid(input.calendarId()));
  }

  @Override
  public void deleteExternal(ExternalBusyCalendarIdentifier input) {
    jdbcTemplate.update(
        "DELETE FROM service_user_external_busy_calendars WHERE service_uid = ? AND user_uid = ? AND ext_calendar_id = ? AND provider = ?",
        toUuid(input.serviceId()),
        toUuid(input.userId()),
        input.extCalendarId(),
        providerToDb(input.provider()));
  }

  @Override
  public List<BusyCalendarProvider> find(ID serviceId, ID userId) {
    return jdbcTemplate.query(
        """
            SELECT ext_c.provider as provider, ext_c.ext_calendar_id as calendar_id
            FROM service_user_external_busy_calendars AS ext_c
            WHERE ext_c.service_uid = ? AND ext_c.user_uid = ?
            UNION ALL
            SELECT 'nittei' as provider, bc.calendar_uid::text as calendar_id
            FROM service_user_busy_calendars AS bc
            WHERE bc.service_uid = ? AND bc.user_uid = ?
            """,
        (rs, rowNum) ->
            new BusyCalendarProvider(rs.getString("provider"), rs.getString("calendar_id")),
        toUuid(serviceId),
        toUuid(userId),
        toUuid(serviceId),
        toUuid(userId));
  }

  private static String providerToDb(IntegrationProvider provider) {
    return switch (provider) {
      case GOOGLE -> "google";
      case OUTLOOK -> "outlook";
    };
  }

  private static UUID toUuid(ID id) {
    return UUID.fromString(id.value());
  }
}
