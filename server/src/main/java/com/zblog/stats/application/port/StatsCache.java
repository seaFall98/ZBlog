package com.zblog.stats.application.port;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public interface StatsCache {

  int articleViewDedupSeconds();

  int collectRateLimitPerMinute();

  int siteStatsCacheSeconds();

  long incrementCollectRate(String visitorId, Duration ttl);

  void invalidateSiteStats();

  boolean markArticleViewIfAbsent(long articleId, String visitorId, Duration ttl);

  void incrementHotArticle(long articleId, double score);

  Optional<Map<String, Object>> siteStats();

  void cacheSiteStats(Map<String, Object> stats, Duration ttl);
}
