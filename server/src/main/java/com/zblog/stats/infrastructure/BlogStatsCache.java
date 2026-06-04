package com.zblog.stats.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.cache.BlogCache;
import com.zblog.cache.CacheProperties;
import com.zblog.common.exception.BusinessException;
import com.zblog.stats.application.port.StatsCache;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class BlogStatsCache implements StatsCache {

  private static final String SITE_STATS_KEY = "zblog:stats:site";

  private final BlogCache blogCache;
  private final CacheProperties cacheProperties;
  private final ObjectMapper objectMapper;

  public BlogStatsCache(BlogCache blogCache, CacheProperties cacheProperties, ObjectMapper objectMapper) {
    this.blogCache = blogCache;
    this.cacheProperties = cacheProperties;
    this.objectMapper = objectMapper;
  }

  public int articleViewDedupSeconds() {
    return cacheProperties.getArticleViewDedupSeconds();
  }

  public int collectRateLimitPerMinute() {
    return cacheProperties.getCollectRateLimitPerMinute();
  }

  public int siteStatsCacheSeconds() {
    return cacheProperties.getSiteStatsCacheSeconds();
  }

  public long incrementCollectRate(String visitorId, Duration ttl) {
    return blogCache.incrementWithTtl("zblog:collect:rate:" + visitorId, ttl);
  }

  public void invalidateSiteStats() {
    blogCache.delete(SITE_STATS_KEY);
  }

  public boolean markArticleViewIfAbsent(long articleId, String visitorId, Duration ttl) {
    return blogCache.setIfAbsent("zblog:article:view:dedup:v2:" + articleId + ":" + visitorId, "1", ttl);
  }

  public void incrementHotArticle(long articleId, double score) {
    blogCache.incrementHotArticle(articleId, score);
  }

  public Optional<Map<String, Object>> siteStats() {
    return blogCache.get(SITE_STATS_KEY).map(this::readStats);
  }

  public void cacheSiteStats(Map<String, Object> stats, Duration ttl) {
    blogCache.set(SITE_STATS_KEY, writeStats(stats), ttl);
  }

  private Map<String, Object> readStats(String value) {
    try {
      return objectMapper.readValue(value, new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      throw new BusinessException(500, "Invalid site stats cache", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private String writeStats(Map<String, Object> stats) {
    try {
      return objectMapper.writeValueAsString(stats);
    } catch (JsonProcessingException exception) {
      throw new BusinessException(500, "Invalid site stats cache", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
