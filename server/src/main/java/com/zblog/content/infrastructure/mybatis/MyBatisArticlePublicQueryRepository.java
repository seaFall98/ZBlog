package com.zblog.content.infrastructure.mybatis;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.content.application.port.ArticlePublicQueryRepository;
import java.sql.Timestamp;
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
public class MyBatisArticlePublicQueryRepository implements ArticlePublicQueryRepository {

  private final ArticlePublicQueryMapper articlePublicQueryMapper;

  public MyBatisArticlePublicQueryRepository(ArticlePublicQueryMapper articlePublicQueryMapper) {
    this.articlePublicQueryMapper = articlePublicQueryMapper;
  }

  public PageResponse<Map<String, Object>> listPublic(
      int page, int pageSize, String category, String tag, String year, String month) {
    PublicFilters filters = filters(category, tag, year, month);
    int offset = (page - 1) * pageSize;
    long total =
        articlePublicQueryMapper.countPublic(
            filters.category(), filters.tag(), filters.year(), filters.month());
    List<Map<String, Object>> rows =
        articlePublicQueryMapper.listPublic(
            filters.category(), filters.tag(), filters.year(), filters.month(), pageSize, offset);
    return new PageResponse<>(withRelations(rows), total, page, pageSize);
  }

  public Map<String, Object> getPublicBySlug(String slug) {
    List<Map<String, Object>> rows = articlePublicQueryMapper.findPublicBySlug(slug);
    if (rows.isEmpty()) {
      throw new BusinessException(404, "Article not found", HttpStatus.NOT_FOUND);
    }
    return withRelations(rows).getFirst();
  }

  public String randomPublishedSlug() {
    String slug = articlePublicQueryMapper.findFirstPublishedSlug();
    if (slug == null) {
      throw new BusinessException(404, "Article not found", HttpStatus.NOT_FOUND);
    }
    return slug;
  }

  public PageResponse<Map<String, Object>> searchPublic(String keyword, int page, int pageSize) {
    int offset = (page - 1) * pageSize;
    String normalized = normalizedLike(keyword);
    long total = articlePublicQueryMapper.countSearchPublic(normalized);
    List<Map<String, Object>> rows = articlePublicQueryMapper.searchPublic(normalized, pageSize, offset);
    return new PageResponse<>(withRelations(rows), total, page, pageSize);
  }

  private PublicFilters filters(String category, String tag, String year, String month) {
    return new PublicFilters(blankToNull(category), blankToNull(tag), parseNullableInt(year), parseNullableInt(month));
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
    return articlePublicQueryMapper.findTagsByArticleIds(articleIds).stream()
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

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private Integer parseNullableInt(String value) {
    String normalized = blankToNull(value);
    return normalized == null ? null : Integer.parseInt(normalized);
  }

  private record PublicFilters(String category, String tag, Integer year, Integer month) {}
}
