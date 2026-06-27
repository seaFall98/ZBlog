package com.zblog.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class InMemoryBlogCache implements BlogCache {

  private final Map<String, Entry> values = new ConcurrentHashMap<>();
  private final Map<Long, Double> hotArticles = new ConcurrentHashMap<>();

  @Override
  public boolean setIfAbsent(String key, String value, Duration ttl) {
    purgeExpired(key);
    Entry entry = new Entry(value, Instant.now().plus(ttl));
    return values.putIfAbsent(key, entry) == null;
  }

  @Override
  public long incrementByWithTtl(String key, long delta, Duration ttl) {
    purgeExpired(key);
    Entry entry =
        values.compute(
            key,
            (ignored, existing) -> {
              long current = existing == null ? 0 : parseLong(existing.value());
              return new Entry(Long.toString(current + delta), Instant.now().plus(ttl));
            });
    return Long.parseLong(entry.value());
  }

  @Override
  public void incrementHotArticle(long articleId, double score) {
    hotArticles.merge(articleId, score, Double::sum);
  }

  @Override
  public List<Long> topHotArticles(int limit) {
    return hotArticles.entrySet().stream()
        .sorted(Map.Entry.<Long, Double>comparingByValue(Comparator.reverseOrder()))
        .limit(limit)
        .map(Map.Entry::getKey)
        .toList();
  }

  @Override
  public Map<Long, Double> hotArticleScores(List<Long> articleIds) {
    Map<Long, Double> scores = new ConcurrentHashMap<>();
    for (Long articleId : articleIds) {
      Double score = hotArticles.get(articleId);
      if (score != null) {
        scores.put(articleId, score);
      }
    }
    return scores;
  }

  @Override
  public Optional<String> get(String key) {
    purgeExpired(key);
    Entry entry = values.get(key);
    return entry == null ? Optional.empty() : Optional.of(entry.value());
  }

  @Override
  public void set(String key, String value, Duration ttl) {
    values.put(key, new Entry(value, Instant.now().plus(ttl)));
  }

  @Override
  public void delete(String key) {
    values.remove(key);
  }

  @Override
  public Map<String, String> scanByPrefix(String prefix) {
    Instant now = Instant.now();
    Map<String, String> result = new ConcurrentHashMap<>();
    values.forEach(
        (key, entry) -> {
          if (key.startsWith(prefix) && entry.expiresAt().isAfter(now)) {
            result.put(key, entry.value());
          }
        });
    return result;
  }

  private void purgeExpired(String key) {
    Entry entry = values.get(key);
    if (entry != null && entry.expiresAt().isBefore(Instant.now())) {
      values.remove(key);
    }
  }

  private long parseLong(String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException exception) {
      return 0;
    }
  }

  private record Entry(String value, Instant expiresAt) {}
}
