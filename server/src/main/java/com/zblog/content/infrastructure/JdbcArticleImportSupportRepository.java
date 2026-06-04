package com.zblog.content.infrastructure;

import com.zblog.content.application.TaxonomyEnsureResult;
import com.zblog.content.application.port.ArticleImportSupportRepository;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcArticleImportSupportRepository implements ArticleImportSupportRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcArticleImportSupportRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public TaxonomyEnsureResult ensureCategory(String name, String slug) {
    List<Long> existing =
        jdbcTemplate.query("select id from categories where slug = ?", (rs, rowNum) -> rs.getLong("id"), slug);
    if (!existing.isEmpty()) {
      return new TaxonomyEnsureResult(existing.getFirst(), false);
    }
    return new TaxonomyEnsureResult(
        insertAndReturnId(
            "insert into categories (name, slug, description, sort_order) values (?, ?, '', 0)", name, slug),
        true);
  }

  public TaxonomyEnsureResult ensureTag(String name, String slug) {
    List<Long> existing =
        jdbcTemplate.query("select id from tags where slug = ?", (rs, rowNum) -> rs.getLong("id"), slug);
    if (!existing.isEmpty()) {
      return new TaxonomyEnsureResult(existing.getFirst(), false);
    }
    return new TaxonomyEnsureResult(
        insertAndReturnId("insert into tags (name, slug, description) values (?, ?, '')", name, slug),
        true);
  }

  public boolean articleSlugExists(String slug) {
    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject("select count(*) > 0 from articles where slug = ?", Boolean.class, slug));
  }

  private long insertAndReturnId(String sql, Object... args) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          for (int i = 0; i < args.length; i++) {
            statement.setObject(i + 1, args[i]);
          }
          return statement;
        },
        keyHolder);
    Map<String, Object> keys = keyHolder.getKeys();
    if (keys != null && keys.get("id") instanceof Number number) {
      return number.longValue();
    }
    return keyHolder.getKey().longValue();
  }
}
