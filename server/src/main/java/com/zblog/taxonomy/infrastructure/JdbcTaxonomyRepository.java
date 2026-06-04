package com.zblog.taxonomy.infrastructure;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.taxonomy.application.port.TaxonomyRepository;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTaxonomyRepository implements TaxonomyRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcTaxonomyRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public PageResponse<Map<String, Object>> listCategories(int page, int pageSize) {
    int offset = (page - 1) * pageSize;
    List<Map<String, Object>> list =
        jdbcTemplate.queryForList(
            """
            select c.id, c.name, c.slug, c.description, c.sort_order as sort,
              count(a.id) as count,
              '/category/' || c.slug as url
            from categories c
            left join articles a on a.category_id = c.id and a.status = 'PUBLISHED'
            group by c.id, c.name, c.slug, c.description, c.sort_order
            order by c.sort_order, c.id
            limit ? offset ?
            """,
            pageSize,
            offset);
    Long total = jdbcTemplate.queryForObject("select count(*) from categories", Long.class);
    return new PageResponse<>(list, total == null ? 0 : total, page, pageSize);
  }

  public Map<String, Object> getCategory(String idOrSlug) {
    List<Map<String, Object>> list =
        jdbcTemplate.queryForList(
            """
            select c.id, c.name, c.slug, c.description, c.sort_order as sort,
              count(a.id) as count,
              '/category/' || c.slug as url
            from categories c
            left join articles a on a.category_id = c.id and a.status = 'PUBLISHED'
            where c.slug = ? or cast(c.id as varchar) = ?
            group by c.id, c.name, c.slug, c.description, c.sort_order
            """,
            idOrSlug,
            idOrSlug);
    return one(list, "Category not found");
  }

  public Map<String, Object> createCategory(
      String name, String slug, String description, int sort) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  "insert into categories (name, slug, description, sort_order) values (?, ?, ?, ?)",
                  Statement.RETURN_GENERATED_KEYS);
          statement.setString(1, name);
          statement.setString(2, slug);
          statement.setString(3, description);
          statement.setInt(4, sort);
          return statement;
        },
        keyHolder);
    return getCategory(String.valueOf(generatedId(keyHolder)));
  }

  public Map<String, Object> updateCategory(
      long id, String name, String slug, String description, int sort) {
    jdbcTemplate.update(
        """
        update categories
        set name = ?, slug = ?, description = ?, sort_order = ?, updated_at = current_timestamp
        where id = ?
        """,
        name,
        slug,
        description,
        sort,
        id);
    return getCategory(String.valueOf(id));
  }

  public void deleteCategory(long id) {
    jdbcTemplate.update("delete from categories where id = ?", id);
  }

  public PageResponse<Map<String, Object>> listTags(int page, int pageSize) {
    int offset = (page - 1) * pageSize;
    List<Map<String, Object>> list =
        jdbcTemplate.queryForList(
            """
            select t.id, t.name, t.slug, t.description,
              count(at.article_id) as count,
              '/tag/' || t.slug as url
            from tags t
            left join article_tags at on at.tag_id = t.id
            left join articles a on a.id = at.article_id and a.status = 'PUBLISHED'
            group by t.id, t.name, t.slug, t.description
            order by t.id
            limit ? offset ?
            """,
            pageSize,
            offset);
    Long total = jdbcTemplate.queryForObject("select count(*) from tags", Long.class);
    return new PageResponse<>(list, total == null ? 0 : total, page, pageSize);
  }

  public Map<String, Object> getTag(String idOrSlug) {
    List<Map<String, Object>> list =
        jdbcTemplate.queryForList(
            """
            select t.id, t.name, t.slug, t.description,
              count(at.article_id) as count,
              '/tag/' || t.slug as url
            from tags t
            left join article_tags at on at.tag_id = t.id
            left join articles a on a.id = at.article_id and a.status = 'PUBLISHED'
            where t.slug = ? or cast(t.id as varchar) = ?
            group by t.id, t.name, t.slug, t.description
            """,
            idOrSlug,
            idOrSlug);
    return one(list, "Tag not found");
  }

  public Map<String, Object> createTag(String name, String slug, String description) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  "insert into tags (name, slug, description) values (?, ?, ?)",
                  Statement.RETURN_GENERATED_KEYS);
          statement.setString(1, name);
          statement.setString(2, slug);
          statement.setString(3, description);
          return statement;
        },
        keyHolder);
    return getTag(String.valueOf(generatedId(keyHolder)));
  }

  public Map<String, Object> updateTag(long id, String name, String slug, String description) {
    jdbcTemplate.update(
        "update tags set name = ?, slug = ?, description = ?, updated_at = current_timestamp where id = ?",
        name,
        slug,
        description,
        id);
    return getTag(String.valueOf(id));
  }

  public void deleteTag(long id) {
    jdbcTemplate.update("delete from tags where id = ?", id);
  }

  private Map<String, Object> one(List<Map<String, Object>> list, String message) {
    if (list.isEmpty()) {
      throw new BusinessException(404, message, HttpStatus.NOT_FOUND);
    }
    return list.getFirst();
  }

  private long generatedId(KeyHolder keyHolder) {
    Map<String, Object> keys = keyHolder.getKeys();
    if (keys != null && keys.get("id") instanceof Number number) {
      return number.longValue();
    }
    return keyHolder.getKey().longValue();
  }
}
