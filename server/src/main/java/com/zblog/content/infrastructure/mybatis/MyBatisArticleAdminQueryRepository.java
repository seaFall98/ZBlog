package com.zblog.content.infrastructure.mybatis;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.content.application.port.ArticleAdminQueryRepository;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisArticleAdminQueryRepository implements ArticleAdminQueryRepository {

  private final ArticleAdminQueryMapper articleAdminQueryMapper;

  public MyBatisArticleAdminQueryRepository(ArticleAdminQueryMapper articleAdminQueryMapper) {
    this.articleAdminQueryMapper = articleAdminQueryMapper;
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
    QueryFilters filters = filters(keyword, published, categoryId, tagIds, location, top, essence, outdated, startTime, endTime);
    int offset = (page - 1) * pageSize;
    long total =
        articleAdminQueryMapper.countAdmin(
            filters.keyword(),
            filters.publishedStatus(),
            filters.categoryId(),
            filters.tagIds(),
            filters.location(),
            filters.top(),
            filters.essence(),
            filters.outdated(),
            filters.start(),
            filters.end());
    List<Map<String, Object>> rows =
        articleAdminQueryMapper.listAdmin(
            filters.keyword(),
            filters.publishedStatus(),
            filters.categoryId(),
            filters.tagIds(),
            filters.location(),
            filters.top(),
            filters.essence(),
            filters.outdated(),
            filters.start(),
            filters.end(),
            pageSize,
            offset);
    return new PageResponse<>(withRelations(rows), total, page, pageSize);
  }

  public Map<String, Object> getAdmin(long id) {
    List<Map<String, Object>> rows = articleAdminQueryMapper.findAdminById(id);
    if (rows.isEmpty()) {
      throw new BusinessException(404, "Article not found", HttpStatus.NOT_FOUND);
    }
    return withRelations(rows).getFirst();
  }

  private QueryFilters filters(
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
    LocalDate start = parseNullableDate(startTime);
    LocalDate end = parseNullableDate(endTime);
    return new QueryFilters(
        normalizedLike(keyword),
        published == null ? null : (published ? "PUBLISHED" : "DRAFT"),
        categoryId,
        normalizedTagIds(tagIds),
        normalizedLike(location),
        top,
        essence,
        outdated,
        start == null ? null : start.atStartOfDay(),
        end == null ? null : end.plusDays(1).atStartOfDay());
  }

  private List<Map<String, Object>> withRelations(List<Map<String, Object>> rows) {
    List<Map<String, Object>> articles = rows.stream().map(LinkedHashMap::new).map(map -> (Map<String, Object>) map).toList();
    Map<Long, List<Map<String, Object>>> tagsByArticleId = findTagsByArticleIds(articleIds(articles));
    for (Map<String, Object> article : articles) {
      long articleId = ((Number) article.get("id")).longValue();
      article.put("is_publish", isPublished(article));
      article.put(
          "category",
          value(article, "category_id", "categoryId") == null
              ? null
              : Map.of(
                  "id",
                  value(article, "category_id", "categoryId"),
                  "name",
                  value(article, "category_name", "categoryName"),
                  "url",
                  "/category/" + value(article, "category_slug", "categorySlug")));
      article.put("tags", tagsByArticleId.getOrDefault(articleId, List.of()));
      renameIfPresent(article, "contentMarkdown", "content_markdown");
      renameIfPresent(article, "isTop", "is_top");
      renameIfPresent(article, "isEssence", "is_essence");
      renameIfPresent(article, "isOutdated", "is_outdated");
      renameIfPresent(article, "viewCount", "view_count");
      renameIfPresent(article, "commentCount", "comment_count");
      renameIfPresent(article, "publishTime", "publish_time");
      renameIfPresent(article, "updateTime", "update_time");
      article.putIfAbsent("cover", null);
      article.putIfAbsent("publish_time", null);
      article.putIfAbsent("update_time", null);
      article.remove("status");
      article.remove("category_id");
      article.remove("categoryId");
      article.remove("category_name");
      article.remove("categoryName");
      article.remove("category_slug");
      article.remove("categorySlug");
      article.computeIfPresent("publish_time", (ignored, value) -> formatTime(value));
      article.computeIfPresent("update_time", (ignored, value) -> formatTime(value));
    }
    return articles;
  }

  private List<Long> articleIds(List<Map<String, Object>> articles) {
    return articles.stream().map(article -> ((Number) article.get("id")).longValue()).toList();
  }

  private boolean isPublished(Map<String, Object> article) {
    Object status = value(article, "status", "STATUS");
    if (status != null) {
      return "PUBLISHED".equals(status.toString());
    }
    Object published = value(article, "is_publish", "isPublish", "IS_PUBLISH");
    if (published instanceof Boolean bool) {
      return bool;
    }
    return published != null && "true".equalsIgnoreCase(published.toString());
  }

  private Object value(Map<String, Object> row, String... keys) {
    for (String key : keys) {
      if (row.containsKey(key)) {
        return row.get(key);
      }
    }
    for (String key : keys) {
      for (Map.Entry<String, Object> entry : row.entrySet()) {
        if (entry.getKey().equalsIgnoreCase(key)) {
          return entry.getValue();
        }
      }
    }
    return null;
  }

  private void renameIfPresent(Map<String, Object> row, String sourceKey, String targetKey) {
    if (row.containsKey(sourceKey)) {
      row.put(targetKey, row.remove(sourceKey));
    }
  }

  private String formatTime(Object value) {
    if (value instanceof Timestamp timestamp) {
      return timestamp.toInstant().toString();
    }
    if (value instanceof java.sql.Date date) {
      return date.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toString();
    }
    if (value instanceof java.util.Date date) {
      return date.toInstant().toString();
    }
    if (value instanceof LocalDateTime dateTime) {
      return dateTime.atZone(ZoneId.systemDefault()).toInstant().toString();
    }
    return value.toString();
  }

  private Map<Long, List<Map<String, Object>>> findTagsByArticleIds(List<Long> articleIds) {
    if (articleIds.isEmpty()) {
      return Map.of();
    }
    return articleAdminQueryMapper.findTagsByArticleIds(articleIds).stream()
        .collect(
            Collectors.groupingBy(
                row -> ((Number) value(row, "article_id", "articleId")).longValue(),
                LinkedHashMap::new,
                Collectors.mapping(this::tagView, Collectors.toCollection(ArrayList::new))));
  }

  private Map<String, Object> tagView(Map<String, Object> row) {
    Map<String, Object> tag = new LinkedHashMap<>();
    tag.put("id", row.get("id"));
    tag.put("name", row.get("name"));
    tag.put("url", row.get("url"));
    return tag;
  }

  private String normalizedLike(String value) {
    String normalized = blankToNull(value);
    return normalized == null ? null : "%" + normalized.toLowerCase() + "%";
  }

  private List<Long> normalizedTagIds(List<Long> tagIds) {
    if (tagIds == null || tagIds.isEmpty()) {
      return List.of();
    }
    return tagIds.stream().filter(java.util.Objects::nonNull).toList();
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private LocalDate parseNullableDate(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return LocalDate.parse(value);
  }

  private record QueryFilters(
      String keyword,
      String publishedStatus,
      Long categoryId,
      List<Long> tagIds,
      String location,
      Boolean top,
      Boolean essence,
      Boolean outdated,
      LocalDateTime start,
      LocalDateTime end) {}
}
