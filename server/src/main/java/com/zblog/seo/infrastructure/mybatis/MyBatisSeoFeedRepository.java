package com.zblog.seo.infrastructure.mybatis;

import com.zblog.seo.application.port.SeoFeedRepository;
import com.zblog.seo.application.port.SeoFeedRepository.FeedArticle;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisSeoFeedRepository implements SeoFeedRepository {

  private final SeoFeedMapper seoFeedMapper;

  public MyBatisSeoFeedRepository(SeoFeedMapper seoFeedMapper) {
    this.seoFeedMapper = seoFeedMapper;
  }

  @Override
  public List<FeedArticle> publishedFeedArticles() {
    return seoFeedMapper.publishedFeedArticles().stream()
        .map(
            row ->
                new FeedArticle(
                    text(row.get("slug")),
                    text(row.get("title")),
                    text(row.get("summary")),
                    text(value(row, "cover_url", "coverUrl")),
                    instant(value(row, "published_at", "publishedAt")),
                    instant(value(row, "updated_at", "updatedAt"))))
        .toList();
  }

  private Object value(Map<String, Object> row, String snakeCase, String camelCase) {
    return row.containsKey(snakeCase) ? row.get(snakeCase) : row.get(camelCase);
  }

  private String text(Object value) {
    return value == null ? "" : value.toString();
  }

  private Instant instant(Object value) {
    if (value instanceof Timestamp timestamp) {
      return timestamp.toInstant();
    }
    if (value instanceof Instant instant) {
      return instant;
    }
    return Instant.now();
  }
}
