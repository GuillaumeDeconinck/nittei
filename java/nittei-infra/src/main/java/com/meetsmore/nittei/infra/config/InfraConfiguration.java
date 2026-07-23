package com.meetsmore.nittei.infra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetsmore.nittei.infra.context.NitteiContext;
import com.meetsmore.nittei.infra.repos.*;
import com.meetsmore.nittei.infra.repos.postgres.PostgresAccountIntegrationRepository;
import com.meetsmore.nittei.infra.repos.postgres.PostgresAccountRepository;
import com.meetsmore.nittei.infra.repos.postgres.PostgresCalendarRepository;
import com.meetsmore.nittei.infra.repos.postgres.PostgresCalendarSyncedRepository;
import com.meetsmore.nittei.infra.repos.postgres.PostgresEventReminderGenerationJobsRepository;
import com.meetsmore.nittei.infra.repos.postgres.PostgresEventRepository;
import com.meetsmore.nittei.infra.repos.postgres.PostgresEventSyncedRepository;
import com.meetsmore.nittei.infra.repos.postgres.PostgresReminderRepository;
import com.meetsmore.nittei.infra.repos.postgres.PostgresReservationRepository;
import com.meetsmore.nittei.infra.repos.postgres.PostgresScheduleRepository;
import com.meetsmore.nittei.infra.repos.postgres.PostgresServiceRepository;
import com.meetsmore.nittei.infra.repos.postgres.PostgresServiceUserBusyCalendarRepository;
import com.meetsmore.nittei.infra.repos.postgres.PostgresServiceUserRepository;
import com.meetsmore.nittei.infra.repos.postgres.PostgresStatusRepository;
import com.meetsmore.nittei.infra.repos.postgres.PostgresUserIntegrationRepository;
import com.meetsmore.nittei.infra.repos.postgres.PostgresUserRepository;
import com.meetsmore.nittei.infra.system.SystemClock;
import com.meetsmore.nittei.utils.config.AppConfig;
import com.meetsmore.nittei.utils.secret.RandomSecretGenerator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class InfraConfiguration {

  private static final Logger log = LoggerFactory.getLogger(InfraConfiguration.class);

  private final AppConfig appConfig;
  private final Environment environment;

  public InfraConfiguration(AppConfig appConfig, Environment environment) {
    this.appConfig = appConfig;
    this.environment = environment;
  }

  @Bean
  public HikariDataSource dataSource() {
    HikariConfig hikari = new HikariConfig();
    hikari.setJdbcUrl(convertToJdbcUrl(appConfig.getPg().getDatabaseUrl()));
    hikari.setMinimumIdle(appConfig.getPg().getMinConnections());
    hikari.setMaximumPoolSize(appConfig.getPg().getMaxConnections());
    hikari.setConnectionTimeout(3000L);
    return new HikariDataSource(hikari);
  }

  @Bean
  public JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Bean
  public Flyway flyway(DataSource dataSource) {
    Flyway flyway =
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load();

    if (!appConfig.getPg().isSkipMigrations()) {
      log.info("[repos] Executing migrations");
      flyway.migrate();
      log.info("[repos] Migrations executed");
    } else {
      log.info("[repos] Migrations skipped");
    }

    return flyway;
  }

  @Bean
  public InfraConfig infraConfig() {
    String secret = appConfig.getCreateAccountSecretCode();
    if (secret == null || secret.isBlank()) {
      if (isDebugLikeProfile()) {
        secret = "create_account_dev_secret";
        log.info("[infra config] Using default UNSECURE secret code for dev profile");
      } else {
        secret = RandomSecretGenerator.createRandomSecret(16);
        log.info("[infra config] Generated create account secret code");
      }
    }

    return new InfraConfig(appConfig.getHttpPort(), secret);
  }

  @Bean
  public Repositories repositories(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    return new Repositories(
        new PostgresAccountRepository(jdbcTemplate, objectMapper),
        new PostgresAccountIntegrationRepository(jdbcTemplate),
        new PostgresCalendarRepository(jdbcTemplate, objectMapper),
        new PostgresCalendarSyncedRepository(jdbcTemplate),
        new PostgresEventRepository(jdbcTemplate, objectMapper),
        new PostgresEventReminderGenerationJobsRepository(jdbcTemplate),
        new PostgresEventSyncedRepository(jdbcTemplate),
        new PostgresScheduleRepository(jdbcTemplate, objectMapper),
        new PostgresReminderRepository(jdbcTemplate),
        new PostgresReservationRepository(jdbcTemplate),
        new PostgresServiceRepository(jdbcTemplate, objectMapper),
        new PostgresServiceUserRepository(jdbcTemplate),
        new PostgresServiceUserBusyCalendarRepository(jdbcTemplate),
        new PostgresStatusRepository(jdbcTemplate),
        new PostgresUserRepository(jdbcTemplate, objectMapper),
        new PostgresUserIntegrationRepository(jdbcTemplate));
  }

  @Bean
  public NitteiContext nitteiContext(
      Repositories repositories, InfraConfig infraConfig, SystemClock systemClock) {
    return new NitteiContext(repositories, infraConfig, systemClock);
  }

  @PostConstruct
  public void logConnection() {
    log.info("[repos] Creating postgres connection");
    log.info("[repos] Postgres connection created");
  }

  private boolean isDebugLikeProfile() {
    for (String profile : environment.getActiveProfiles()) {
      if ("dev".equalsIgnoreCase(profile)
          || "local".equalsIgnoreCase(profile)
          || "test".equalsIgnoreCase(profile)) {
        return true;
      }
    }
    return false;
  }

  private String convertToJdbcUrl(String url) {
    if (url == null) {
      return "jdbc:postgresql://localhost:45432/nittei";
    }
    if (url.startsWith("jdbc:")) {
      return url;
    }
    if (url.startsWith("postgresql://")) {
      return "jdbc:" + url;
    }
    return url;
  }
}
