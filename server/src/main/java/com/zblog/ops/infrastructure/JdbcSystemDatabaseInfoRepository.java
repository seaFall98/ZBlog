package com.zblog.ops.infrastructure;

import com.zblog.ops.application.port.SystemDatabaseInfoRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSystemDatabaseInfoRepository implements SystemDatabaseInfoRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcSystemDatabaseInfoRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public String productName() {
    return jdbcTemplate.execute(
        (org.springframework.jdbc.core.ConnectionCallback<String>)
            connection -> connection.getMetaData().getDatabaseProductName());
  }

  public long tableCount() {
    Number value =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.tables
            where lower(table_schema) not in ('information_schema', 'pg_catalog')
            """,
            Number.class);
    return value == null ? 0 : value.longValue();
  }

  public String status() {
    Integer value = jdbcTemplate.queryForObject("select 1", Integer.class);
    return value != null && value == 1 ? "UP" : "DOWN";
  }
}
