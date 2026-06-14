package com.zblog.notification;

import java.time.Duration;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisUnreadNotificationCount {

  private static final Duration TTL = Duration.ofMinutes(10);
  private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

  public RedisUnreadNotificationCount(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
    this.redisTemplateProvider = redisTemplateProvider;
  }

  public long get(long userId, LongSupplier fallback) {
    String key = key(userId);
    try {
      StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
      if (redisTemplate != null) {
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
          return Long.parseLong(cached);
        }
        long count = fallback.getAsLong();
        redisTemplate.opsForValue().set(key, Long.toString(count), TTL);
        return count;
      }
    } catch (RuntimeException ignored) {
      // Redis accelerates unread counts; PostgreSQL remains authoritative.
    }
    return fallback.getAsLong();
  }

  public void invalidate(long userId) {
    try {
      StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
      if (redisTemplate != null) {
        redisTemplate.delete(key(userId));
      }
    } catch (RuntimeException ignored) {
      // Cache failure must not affect notification state.
    }
  }

  private String key(long userId) {
    return "zblog:p3:notifications:unread:" + userId;
  }
}
