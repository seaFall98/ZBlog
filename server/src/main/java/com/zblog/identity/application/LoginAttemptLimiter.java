package com.zblog.identity.application;

import com.zblog.common.exception.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LoginAttemptLimiter {

  private static final int MAX_FAILURES = 5;
  private static final Duration LOCK_DURATION = Duration.ofMinutes(10);
  private static final String KEY_PREFIX = "zblog:auth:login-fail:";

  private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
  private final Clock clock;
  private final Map<String, LocalCounter> localCounters = new ConcurrentHashMap<>();

  @Autowired
  public LoginAttemptLimiter(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
    this(redisTemplateProvider, Clock.systemUTC());
  }

  LoginAttemptLimiter(ObjectProvider<StringRedisTemplate> redisTemplateProvider, Clock clock) {
    this.redisTemplateProvider = redisTemplateProvider;
    this.clock = clock;
  }

  public void assertNotLocked(String email, String remoteAddress) {
    String key = key(email, remoteAddress);
    if (redisLocked(key) || localLocked(key)) {
      throw new BusinessException(429, "Too many login attempts, please try again later", HttpStatus.TOO_MANY_REQUESTS);
    }
  }

  public void recordFailure(String email, String remoteAddress) {
    String key = key(email, remoteAddress);
    if (!recordRedisFailure(key)) {
      recordLocalFailure(key);
    }
  }

  public void clear(String email, String remoteAddress) {
    String key = key(email, remoteAddress);
    try {
      StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
      if (redisTemplate != null) {
        redisTemplate.delete(KEY_PREFIX + key);
      }
    } catch (RuntimeException ignored) {
      // Redis accelerates lock sharing; local fallback remains authoritative for this node.
    }
    localCounters.remove(key);
  }

  private boolean redisLocked(String key) {
    try {
      StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
      if (redisTemplate == null) {
        return false;
      }
      String value = redisTemplate.opsForValue().get(KEY_PREFIX + key);
      return value != null && Integer.parseInt(value) >= MAX_FAILURES;
    } catch (RuntimeException exception) {
      return false;
    }
  }

  private boolean recordRedisFailure(String key) {
    try {
      StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
      if (redisTemplate == null) {
        return false;
      }
      Long count = redisTemplate.opsForValue().increment(KEY_PREFIX + key);
      if (count != null && count == 1) {
        redisTemplate.expire(KEY_PREFIX + key, LOCK_DURATION);
      }
      return true;
    } catch (RuntimeException exception) {
      return false;
    }
  }

  private boolean localLocked(String key) {
    LocalCounter counter = localCounters.get(key);
    if (counter == null) {
      return false;
    }
    if (counter.expiresAt().isBefore(clock.instant())) {
      localCounters.remove(key);
      return false;
    }
    return counter.count() >= MAX_FAILURES;
  }

  private void recordLocalFailure(String key) {
    Instant now = clock.instant();
    localCounters.compute(
        key,
        (ignored, existing) -> {
          if (existing == null || existing.expiresAt().isBefore(now)) {
            return new LocalCounter(1, now.plus(LOCK_DURATION));
          }
          return new LocalCounter(existing.count() + 1, existing.expiresAt());
        });
  }

  private String key(String email, String remoteAddress) {
    String normalizedEmail = StringUtils.hasText(email) ? email.trim().toLowerCase() : "blank";
    String normalizedRemote = StringUtils.hasText(remoteAddress) ? remoteAddress.trim() : "unknown";
    return normalizedEmail + ":" + normalizedRemote;
  }

  private record LocalCounter(int count, Instant expiresAt) {}
}
