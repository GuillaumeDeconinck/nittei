package com.meetsmore.nittei.infra.repos.postgres;

import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.infra.repos.ReservationRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public class PostgresReservationRepository implements ReservationRepository {

  private final JdbcTemplate jdbcTemplate;

  public PostgresReservationRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void increment(ID serviceId, Instant timestamp) {
    jdbcTemplate.update(
        "INSERT INTO service_reservations(service_uid, timestamp) VALUES(?, ?) ON CONFLICT(service_uid, timestamp) DO UPDATE SET count = service_reservations.count + 1",
        toUuid(serviceId),
        Timestamp.from(timestamp));
  }

  @Override
  public void decrement(ID serviceId, Instant timestamp) {
    jdbcTemplate.update(
        "UPDATE service_reservations SET count = count - 1 WHERE service_uid = ? AND timestamp = ?",
        toUuid(serviceId),
        Timestamp.from(timestamp));
  }

  @Override
  public int count(ID serviceId, Instant timestamp) {
    Integer res =
        jdbcTemplate
            .query(
                "SELECT count FROM service_reservations WHERE service_uid = ? AND timestamp = ?",
                (rs, rowNum) -> rs.getInt("count"),
                toUuid(serviceId),
                Timestamp.from(timestamp))
            .stream()
            .findFirst()
            .orElse(0);
    return res == null ? 0 : res;
  }

  private static UUID toUuid(ID id) {
    return UUID.fromString(id.value());
  }
}
