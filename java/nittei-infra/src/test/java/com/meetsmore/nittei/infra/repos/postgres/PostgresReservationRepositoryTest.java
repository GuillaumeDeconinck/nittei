package com.meetsmore.nittei.infra.repos.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PostgresReservationRepositoryTest extends PostgresRepositoryIntegrationTestSupport {

  @Test
  void incrementAndCountPersistReservationRowsInPostgres() {
    var accountId = id("00000000-0000-0000-0000-000000000001");
    var serviceId = id("00000000-0000-0000-0000-000000000002");
    Instant timestamp = Instant.parse("2026-04-20T00:00:00Z");

    insertAccount(accountId);
    insertService(accountId, serviceId);

    PostgresReservationRepository repository = new PostgresReservationRepository(jdbcTemplate);

    assertEquals(0, repository.count(serviceId, timestamp));

    repository.increment(serviceId, timestamp);
    repository.increment(serviceId, timestamp);

    assertEquals(2, repository.count(serviceId, timestamp));
  }

  @Test
  void decrementReducesCountButKeepsRowConsistent() {
    var accountId = id("00000000-0000-0000-0000-000000000011");
    var serviceId = id("00000000-0000-0000-0000-000000000012");
    Instant timestamp = Instant.parse("2026-04-20T12:30:00Z");

    insertAccount(accountId);
    insertService(accountId, serviceId);

    PostgresReservationRepository repository = new PostgresReservationRepository(jdbcTemplate);

    repository.increment(serviceId, timestamp);
    repository.increment(serviceId, timestamp);
    repository.decrement(serviceId, timestamp);

    assertEquals(1, repository.count(serviceId, timestamp));
  }
}
