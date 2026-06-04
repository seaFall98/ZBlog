package com.zblog.event.infrastructure;

import com.zblog.event.application.port.EventConsumptionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcEventConsumptionRepository implements EventConsumptionRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcEventConsumptionRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public boolean alreadyConsumed(long eventId, String consumerName) {
    Long count =
        jdbcTemplate.queryForObject(
            "select count(*) from event_consumptions where event_id = ? and consumer_name = ?",
            Long.class,
            eventId,
            consumerName);
    return count != null && count > 0;
  }

  public void markConsumed(long eventId, String consumerName) {
    jdbcTemplate.update(
        "insert into event_consumptions (event_id, consumer_name) values (?, ?)", eventId, consumerName);
  }
}
