package com.zblog.site.infrastructure;

import com.zblog.site.application.port.SettingRepository;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSettingRepository implements SettingRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcSettingRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Map<String, String> getGroup(String group) {
    return jdbcTemplate
        .queryForList(
            "select key_name, value_text from settings where group_name = ? order by key_name",
            group)
        .stream()
        .collect(
            java.util.stream.Collectors.toMap(
                row -> row.get("key_name").toString(),
                row -> row.get("value_text").toString(),
                (left, right) -> right,
                java.util.LinkedHashMap::new));
  }

  public void upsert(String group, String key, String value) {
    int updated =
        jdbcTemplate.update(
            """
            update settings
            set value_text = ?, updated_at = current_timestamp
            where group_name = ? and key_name = ?
            """,
            value,
            group,
            key);
    if (updated == 0) {
      jdbcTemplate.update(
          "insert into settings (group_name, key_name, value_text) values (?, ?, ?)",
          group,
          key,
          value);
    }
  }
}
