package com.zblog.content.infrastructure;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcArticleRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcArticleRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public PageResponse<Map<String, Object>> listPublic(
      int page, int pageSize, String category, String tag, String year, String month) {
    int offset = (page - 1) * pageSize;
    List<Object> args = new ArrayList<>();
    StringBuilder where = new StringBuilder(" where a.status = 'PUBLISHED'");
    if (category != null && !category.isBlank()) {
      where.append(" and c.slug = ?");
      args.add(category);
    }
    if (tag != null && !tag.isBlank()) {
      where.append(" and exists (select 1 from article_tags at2 join tags t2 on t2.id = at2.tag_id where at2.article_id = a.id and t2.slug = ?)");
      args.add(tag);
    }
    if (year != null && !year.isBlank()) {
      where.append(" and extract(year from a.published_at) = ?");
      args.add(Integer.parseInt(year));
    }
    if (month != null && !month.isBlank()) {
      where.append(" and extract(month from a.published_at) = ?");
      args.add(Integer.parseInt(month));
    }

    Long total =
        jdbcTemplate.queryForObject(
            "select count(*) from articles a left join categories c on c.id = a.category_id"
                + where,
            Long.class,
            args.toArray());
    args.add(pageSize);
    args.add(offset);
    List<Map<String, Object>> articles =
        jdbcTemplate
            .queryForList(baseSelect() + where + " order by a.is_top desc, a.published_at desc, a.id desc limit ? offset ?", args.toArray())
            .stream()
            .map(this::withRelations)
            .toList();
    return new PageResponse<>(articles, total == null ? 0 : total, page, pageSize);
  }

  public Map<String, Object> getPublicBySlug(String slug) {
    List<Map<String, Object>> rows =
        jdbcTemplate.queryForList(baseSelect() + " where a.slug = ? and a.status = 'PUBLISHED'", slug);
    return withRelations(one(rows, "Article not found"));
  }

  public String randomPublishedSlug() {
    List<String> slugs =
        jdbcTemplate.queryForList(
            "select slug from articles where status = 'PUBLISHED' order by id limit 1", String.class);
    if (slugs.isEmpty()) {
      throw new BusinessException(404, "Article not found", HttpStatus.NOT_FOUND);
    }
    return slugs.getFirst();
  }

  public PageResponse<Map<String, Object>> searchPublic(String keyword, int page, int pageSize) {
    int offset = (page - 1) * pageSize;
    String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
    List<Object> args = new ArrayList<>();
    StringBuilder where = new StringBuilder(" where a.status = 'PUBLISHED'");
    if (!normalized.isBlank()) {
      where.append(
          " and (lower(a.title) like ? or lower(a.summary) like ? or lower(a.content_text) like ?)");
      String like = "%" + normalized + "%";
      args.add(like);
      args.add(like);
      args.add(like);
    }
    Long total =
        jdbcTemplate.queryForObject(
            "select count(*) from articles a left join categories c on c.id = a.category_id"
                + where,
            Long.class,
            args.toArray());
    args.add(pageSize);
    args.add(offset);
    List<Map<String, Object>> articles =
        jdbcTemplate
            .queryForList(
                baseSelect() + where + " order by a.published_at desc, a.id desc limit ? offset ?",
                args.toArray())
            .stream()
            .map(this::withRelations)
            .toList();
    return new PageResponse<>(articles, total == null ? 0 : total, page, pageSize);
  }

  public List<Map<String, Object>> hotPublished(int limit) {
    return jdbcTemplate
        .queryForList(
            baseSelect()
                + " where a.status = 'PUBLISHED' order by a.view_count desc, a.published_at desc, a.id desc limit ?",
            limit)
        .stream()
        .map(this::withRelations)
        .toList();
  }

  public List<Map<String, Object>> findPublishedByIds(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
    Map<Long, Map<String, Object>> byId =
        jdbcTemplate
            .queryForList(baseSelect() + " where a.status = 'PUBLISHED' and a.id in (" + placeholders + ")", ids.toArray())
            .stream()
            .map(this::withRelations)
            .collect(java.util.stream.Collectors.toMap(row -> ((Number) row.get("id")).longValue(), row -> row));
    return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
  }

  public PageResponse<Map<String, Object>> listAdmin(
      int page,
      int pageSize,
      String keyword,
      Boolean published,
      Long categoryId,
      List<Long> tagIds,
      String location,
      Boolean top,
      Boolean essence,
      Boolean outdated,
      String startTime,
      String endTime) {
    int offset = (page - 1) * pageSize;
    List<Object> args = new ArrayList<>();
    StringBuilder where = new StringBuilder(" where 1 = 1");
    if (keyword != null && !keyword.isBlank()) {
      where.append(
          " and (lower(a.title) like ? or lower(a.slug) like ? or lower(a.summary) like ? or lower(a.content_text) like ?)");
      String like = "%" + keyword.toLowerCase() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
      args.add(like);
    }
    if (published != null) {
      where.append(" and a.status = ?");
      args.add(published ? "PUBLISHED" : "DRAFT");
    }
    if (categoryId != null) {
      where.append(" and a.category_id = ?");
      args.add(categoryId);
    }
    if (tagIds != null) {
      for (Long tagId : tagIds) {
        if (tagId != null) {
          where.append(" and exists (select 1 from article_tags at2 where at2.article_id = a.id and at2.tag_id = ?)");
          args.add(tagId);
        }
      }
    }
    if (location != null && !location.isBlank()) {
      where.append(" and lower(coalesce(a.location, '')) like ?");
      args.add("%" + location.toLowerCase() + "%");
    }
    if (top != null) {
      where.append(" and a.is_top = ?");
      args.add(top);
    }
    if (essence != null) {
      where.append(" and a.is_essence = ?");
      args.add(essence);
    }
    if (outdated != null) {
      where.append(" and a.is_outdated = ?");
      args.add(outdated);
    }
    LocalDate start = parseNullableDate(startTime);
    LocalDate end = parseNullableDate(endTime);
    if (start != null) {
      where.append(" and coalesce(a.published_at, a.created_at) >= ?");
      args.add(Timestamp.valueOf(start.atStartOfDay()));
    }
    if (end != null) {
      where.append(" and coalesce(a.published_at, a.created_at) < ?");
      args.add(Timestamp.valueOf(end.plusDays(1).atStartOfDay()));
    }
    Long total =
        jdbcTemplate.queryForObject(
            "select count(*) from articles a left join categories c on c.id = a.category_id"
                + where,
            Long.class,
            args.toArray());
    args.add(pageSize);
    args.add(offset);
    List<Map<String, Object>> articles =
        jdbcTemplate
            .queryForList(baseSelect() + where + " order by a.updated_at desc, a.id desc limit ? offset ?", args.toArray())
            .stream()
            .map(this::withRelations)
            .toList();
    return new PageResponse<>(articles, total == null ? 0 : total, page, pageSize);
  }

  public PageResponse<Map<String, Object>> listAdmin(
      int page, int pageSize, String keyword, Boolean published) {
    return listAdmin(
        page,
        pageSize,
        keyword,
        published,
        null,
        List.of(),
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public Map<String, Object> getAdmin(long id) {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(baseSelect() + " where a.id = ?", id);
    return withRelations(one(rows, "Article not found"));
  }

  public Map<String, Object> create(
      String title,
      String slug,
      String markdown,
      String html,
      String text,
      String summary,
      String cover,
      Long categoryId,
      List<Long> tagIds,
      String location,
      boolean top,
      boolean essence,
      boolean outdated) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  """
                  insert into articles (
                    title, slug, content_markdown, content_html, content_text, summary, cover_url,
                    category_id, location, is_top, is_essence, is_outdated, status
                  ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT')
                  """,
                  Statement.RETURN_GENERATED_KEYS);
          statement.setString(1, title);
          statement.setString(2, slug);
          statement.setString(3, markdown);
          statement.setString(4, html);
          statement.setString(5, text);
          statement.setString(6, summary);
          statement.setString(7, cover);
          if (categoryId == null) {
            statement.setObject(8, null);
          } else {
            statement.setLong(8, categoryId);
          }
          statement.setString(9, location);
          statement.setBoolean(10, top);
          statement.setBoolean(11, essence);
          statement.setBoolean(12, outdated);
          return statement;
        },
        keyHolder);
    long id = generatedId(keyHolder);
    replaceTags(id, tagIds);
    return getAdmin(id);
  }

  public Map<String, Object> update(
      long id,
      String title,
      String slug,
      String markdown,
      String html,
      String text,
      String summary,
      String cover,
      Long categoryId,
      List<Long> tagIds,
      String location,
      boolean top,
      boolean essence,
      boolean outdated) {
    jdbcTemplate.update(
        """
        update articles
        set title = ?, slug = ?, content_markdown = ?, content_html = ?, content_text = ?,
            summary = ?, cover_url = ?, category_id = ?, location = ?, is_top = ?,
            is_essence = ?, is_outdated = ?, updated_at = current_timestamp
        where id = ?
        """,
        title,
        slug,
        markdown,
        html,
        text,
        summary,
        cover,
        categoryId,
        location,
        top,
        essence,
        outdated,
        id);
    replaceTags(id, tagIds);
    return getAdmin(id);
  }

  public Map<String, Object> publish(long id) {
    jdbcTemplate.update(
        """
        update articles
        set status = 'PUBLISHED',
            published_at = coalesce(published_at, current_timestamp),
            updated_at = current_timestamp
        where id = ?
        """,
        id);
    return getAdmin(id);
  }

  public Map<String, Object> unpublish(long id) {
    jdbcTemplate.update(
        "update articles set status = 'DRAFT', updated_at = current_timestamp where id = ?", id);
    return getAdmin(id);
  }

  public void delete(long id) {
    jdbcTemplate.update("delete from articles where id = ?", id);
  }

  private void replaceTags(long articleId, List<Long> tagIds) {
    jdbcTemplate.update("delete from article_tags where article_id = ?", articleId);
    for (Long tagId : tagIds) {
      jdbcTemplate.update(
          "insert into article_tags (article_id, tag_id) values (?, ?)", articleId, tagId);
    }
  }

  private String baseSelect() {
    return """
        select a.id, a.title, a.slug, '/posts/' || a.slug as url,
          a.content_html as content, a.content_markdown, a.summary, a.cover_url as cover,
          a.status, a.is_top, a.is_essence, a.is_outdated, a.view_count, a.comment_count,
          a.location, a.published_at as publish_time, a.updated_at as update_time,
          c.id as category_id, c.name as category_name, c.slug as category_slug
        from articles a
        left join categories c on c.id = a.category_id
        """;
  }

  private LocalDate parseNullableDate(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return LocalDate.parse(value);
  }

  private Map<String, Object> withRelations(Map<String, Object> article) {
    long articleId = ((Number) article.get("id")).longValue();
    article.put("is_publish", "PUBLISHED".equals(article.get("status")));
    article.put(
        "category",
        article.get("category_id") == null
            ? null
            : Map.of(
                "id",
                article.get("category_id"),
                "name",
                article.get("category_name"),
                "url",
                "/category/" + article.get("category_slug")));
    article.put("tags", findTags(articleId));
    article.remove("status");
    article.remove("category_id");
    article.remove("category_name");
    article.remove("category_slug");
    Object publishTime = article.get("publish_time");
    if (publishTime instanceof Timestamp timestamp) {
      article.put("publish_time", timestamp.toInstant().toString());
    }
    Object updateTime = article.get("update_time");
    if (updateTime instanceof Timestamp timestamp) {
      article.put("update_time", timestamp.toInstant().toString());
    }
    return article;
  }

  private List<Map<String, Object>> findTags(long articleId) {
    return jdbcTemplate.queryForList(
        """
        select t.id, t.name, '/tag/' || t.slug as url
        from tags t
        join article_tags at on at.tag_id = t.id
        where at.article_id = ?
        order by t.id
        """,
        articleId);
  }

  private Map<String, Object> one(List<Map<String, Object>> rows, String message) {
    if (rows.isEmpty()) {
      throw new BusinessException(404, message, HttpStatus.NOT_FOUND);
    }
    return rows.getFirst();
  }

  private long generatedId(KeyHolder keyHolder) {
    Map<String, Object> keys = keyHolder.getKeys();
    if (keys != null && keys.get("id") instanceof Number number) {
      return number.longValue();
    }
    return keyHolder.getKey().longValue();
  }
}
