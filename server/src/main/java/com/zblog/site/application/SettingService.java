package com.zblog.site.application;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingService {

  private final JdbcTemplate jdbcTemplate;
  private final SecureRandom secureRandom = new SecureRandom();

  public SettingService(JdbcTemplate jdbcTemplate) {
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

  @Transactional
  public Map<String, String> updateGroup(String group, Map<String, String> values) {
    values.forEach((key, value) -> upsert(group, key, value == null ? "" : value));
    return getGroup(group);
  }

  public Map<String, String> resetMcpSecret() {
    byte[] bytes = new byte[24];
    secureRandom.nextBytes(bytes);
    String secret = HexFormat.of().formatHex(bytes);
    upsert("ai", "mcp_secret", secret);
    return Map.of("secret", secret);
  }

  private void upsert(String group, String key, String value) {
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
