package com.zblog.cache;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface BlogCache {

  boolean setIfAbsent(String key, String value, Duration ttl);

  default long incrementWithTtl(String key, Duration ttl) {
    return incrementByWithTtl(key, 1, ttl);
  }

  long incrementByWithTtl(String key, long delta, Duration ttl);

  void incrementHotArticle(long articleId, double score);

  List<Long> topHotArticles(int limit);

  Map<Long, Double> hotArticleScores(List<Long> articleIds);

  Optional<String> get(String key);

  void set(String key, String value, Duration ttl);

  void delete(String key);

  default Map<String, String> scanByPrefix(String prefix) {
    return Map.of();
  }
}
