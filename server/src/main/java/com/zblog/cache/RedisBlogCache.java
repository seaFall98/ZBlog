package com.zblog.cache;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class RedisBlogCache implements BlogCache {

  private static final String HOT_ARTICLES_KEY = "zblog:article:hot";

  private final StringRedisTemplate redisTemplate;

  public RedisBlogCache(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public boolean setIfAbsent(String key, String value, Duration ttl) {
    try {
      Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
      return Boolean.TRUE.equals(result);
    } catch (RuntimeException exception) {
      return true;
    }
  }

  @Override
  public long incrementWithTtl(String key, Duration ttl) {
    try {
      Long value = redisTemplate.opsForValue().increment(key);
      if (value != null && value == 1L) {
        redisTemplate.expire(key, ttl);
      }
      return value == null ? 1 : value;
    } catch (RuntimeException exception) {
      return 1;
    }
  }

  @Override
  public void incrementHotArticle(long articleId, double score) {
    try {
      redisTemplate.opsForZSet().incrementScore(HOT_ARTICLES_KEY, Long.toString(articleId), score);
    } catch (RuntimeException ignored) {
      // Redis is an accelerator; PostgreSQL remains the fallback source.
    }
  }

  @Override
  public List<Long> topHotArticles(int limit) {
    try {
      Set<String> ids = redisTemplate.opsForZSet().reverseRange(HOT_ARTICLES_KEY, 0, Math.max(0, limit - 1));
      if (ids == null) {
        return List.of();
      }
      return ids.stream().map(Long::parseLong).toList();
    } catch (RuntimeException exception) {
      return List.of();
    }
  }

  @Override
  public Map<Long, Double> hotArticleScores(List<Long> articleIds) {
    Map<Long, Double> scores = new LinkedHashMap<>();
    try {
      for (Long articleId : articleIds) {
        Double score = redisTemplate.opsForZSet().score(HOT_ARTICLES_KEY, Long.toString(articleId));
        if (score != null) {
          scores.put(articleId, score);
        }
      }
    } catch (RuntimeException exception) {
      return Map.of();
    }
    return scores;
  }

  @Override
  public Optional<String> get(String key) {
    try {
      return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    } catch (RuntimeException exception) {
      return Optional.empty();
    }
  }

  @Override
  public void set(String key, String value, Duration ttl) {
    try {
      redisTemplate.opsForValue().set(key, value, ttl);
    } catch (RuntimeException ignored) {
      // Cache writes must not break core blog behavior.
    }
  }

  @Override
  public void delete(String key) {
    try {
      redisTemplate.delete(key);
    } catch (RuntimeException ignored) {
      // Cache eviction is best-effort.
    }
  }
}
