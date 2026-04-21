package com.meetsmore.nittei.infra.repos.postgres;

import com.meetsmore.nittei.infra.repos.StatusRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class PostgresStatusRepository implements StatusRepository {

  private final JdbcTemplate jdbcTemplate;

  public PostgresStatusRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void checkConnection() {
    try {
      jdbcTemplate.queryForObject("SELECT 1", Integer.class);
    } catch (EmptyResultDataAccessException ignored) {
      // No-op
    }
  }
}
