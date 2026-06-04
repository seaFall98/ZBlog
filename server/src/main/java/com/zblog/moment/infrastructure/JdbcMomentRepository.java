package com.zblog.moment.infrastructure;

import com.zblog.common.exception.BusinessException;
import com.zblog.moment.application.port.MomentRepository;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcMomentRepository implements MomentRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcMomentRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Map<String, Object>> findAll() {
    return jdbcTemplate.queryForList("select * from moments");
  }

  public Map<String, Object> find(long id) {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList("select * from moments where id = ?", id);
    if (rows.isEmpty()) {
      throw new BusinessException(404, "Moment not found", HttpStatus.NOT_FOUND);
    }
    return rows.getFirst();
  }

  @Transactional
  public long create(String contentJson, boolean publish, Instant publishTime) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  "insert into moments (content_json, is_publish, publish_time) values (?, ?, ?)",
                  Statement.RETURN_GENERATED_KEYS);
          statement.setString(1, contentJson);
          statement.setBoolean(2, publish);
          statement.setTimestamp(3, timestamp(publishTime));
          return statement;
        },
        keyHolder);
    return generatedId(keyHolder);
  }

  @Transactional
  public void update(long id, String contentJson, boolean publish, Instant publishTime) {
    jdbcTemplate.update(
        "update moments set content_json = ?, is_publish = ?, publish_time = ?, updated_at = current_timestamp where id = ?",
        contentJson,
        publish,
        publishTime,
        id);
  }

  @Transactional
  public void delete(long id) {
    jdbcTemplate.update("delete from moments where id = ?", id);
  }

  private Timestamp timestamp(Instant value) {
    return value == null ? null : Timestamp.from(value);
  }

  private long generatedId(KeyHolder keyHolder) {
    Map<String, Object> keys = keyHolder.getKeys();
    if (keys != null && keys.get("id") instanceof Number number) {
      return number.longValue();
    }
    return keyHolder.getKey().longValue();
  }
}
