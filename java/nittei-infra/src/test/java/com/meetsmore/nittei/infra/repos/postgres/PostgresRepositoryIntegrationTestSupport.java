package com.meetsmore.nittei.infra.repos.postgres;

import com.meetsmore.nittei.domain.ID;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
abstract class PostgresRepositoryIntegrationTestSupport {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:15.4-alpine")
          .withDatabaseName("nittei")
          .withUsername("postgres")
          .withPassword("postgres");

  private static HikariDataSource dataSource;

  protected JdbcTemplate jdbcTemplate;

  @BeforeAll
  static void setUpDatabase() {
    POSTGRES.start();

    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(POSTGRES.getJdbcUrl());
    hikariConfig.setUsername(POSTGRES.getUsername());
    hikariConfig.setPassword(POSTGRES.getPassword());
    hikariConfig.setMaximumPoolSize(2);
    hikariConfig.setMinimumIdle(1);
    dataSource = new HikariDataSource(hikariConfig);

    Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
  }

  @AfterAll
  static void tearDownDatabase() {
    if (dataSource != null) {
      dataSource.close();
    }
  }

  @BeforeEach
  void setUpJdbcTemplate() {
    jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.execute(
        """
            TRUNCATE TABLE
              reminders,
              calendar_event_reminder_generation_jobs,
              event_reminder_versions,
              calendar_events,
              service_reservations,
              calendars,
              services,
              users,
              accounts
            RESTART IDENTITY CASCADE
            """);
  }

  protected DataSource dataSource() {
    return dataSource;
  }

  protected void insertAccount(ID accountId) {
    jdbcTemplate.update(
        "INSERT INTO accounts(account_uid, secret_api_key, public_jwt_key, settings) VALUES (?, ?, ?, '{}'::jsonb)",
        uuid(accountId),
        "secret-" + accountId.value(),
        null);
  }

  protected void insertUser(ID accountId, ID userId) {
    jdbcTemplate.update(
        "INSERT INTO users(user_uid, account_uid, metadata) VALUES (?, ?, '{}'::jsonb)",
        uuid(userId),
        uuid(accountId));
  }

  protected void insertService(ID accountId, ID serviceId) {
    jdbcTemplate.update(
        "INSERT INTO services(service_uid, account_uid, multi_person, metadata) VALUES (?, ?, '{}'::json, '{}'::jsonb)",
        uuid(serviceId),
        uuid(accountId));
  }

  protected void insertCalendar(ID accountId, ID userId, ID calendarId) {
    jdbcTemplate.update(
        "INSERT INTO calendars(calendar_uid, user_uid, settings, metadata, account_uid) VALUES (?, ?, '{}'::json, '{}'::jsonb, ?)",
        uuid(calendarId),
        uuid(userId),
        uuid(accountId));
  }

  protected void insertCalendarEvent(
      ID accountId,
      ID userId,
      ID eventId,
      ID calendarId,
      ID serviceId,
      Instant start,
      Instant end) {
    jdbcTemplate.update(
        """
            INSERT INTO calendar_events(
              event_uid,
              account_uid,
              user_uid,
              calendar_uid,
              external_parent_id,
              external_id,
              title,
              description,
              event_type,
              location,
              status,
              all_day,
              start_time,
              duration,
              busy,
              end_time,
              created,
              updated,
              recurrence_jsonb,
              recurring_until,
              exdates,
              recurring_event_uid,
              original_start_time,
              reminders_jsonb,
              service_uid,
              metadata
            ) VALUES (?, ?, ?, ?, NULL, NULL, NULL, NULL, NULL, NULL, 'confirmed', FALSE, ?, ?, TRUE, ?, ?, ?, NULL, NULL, ARRAY[]::timestamptz[], NULL, NULL, '[]'::jsonb, ?, '{}'::jsonb)
            """,
        uuid(eventId),
        uuid(accountId),
        uuid(userId),
        uuid(calendarId),
        Timestamp.from(start),
        end.toEpochMilli() - start.toEpochMilli(),
        Timestamp.from(end),
        Instant.now().toEpochMilli(),
        Instant.now().toEpochMilli(),
        serviceId == null ? null : uuid(serviceId));
  }

  protected void insertEventReminderVersion(ID eventId, long version) {
    jdbcTemplate.update(
        "INSERT INTO event_reminder_versions(event_uid, version) VALUES (?, ?)",
        uuid(eventId),
        version);
  }

  protected static ID id(String value) {
    return new ID(value);
  }

  private static UUID uuid(ID id) {
    return UUID.fromString(id.value());
  }
}
