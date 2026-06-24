package com.zblog.stats.application;

import com.zblog.cache.BlogCache;
import com.zblog.stats.application.port.VisitRepository;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ArticleViewCountBuffer {

  private static final String KEY_PREFIX = "zblog:article:view:pending:";
  private static final Duration PENDING_TTL = Duration.ofDays(7);

  private final BlogCache blogCache;
  private final VisitRepository visitRepository;

  public ArticleViewCountBuffer(BlogCache blogCache, VisitRepository visitRepository) {
    this.blogCache = blogCache;
    this.visitRepository = visitRepository;
  }

  public void increment(long articleId) {
    blogCache.incrementWithTtl(key(articleId), PENDING_TTL);
    blogCache.incrementHotArticle(articleId, 1);
  }

  public long visibleCount(long articleId) {
    Long persisted = visitRepository.articleViewCount(articleId);
    return (persisted == null ? 0 : persisted) + pendingDelta(articleId);
  }

  public long pendingDeltaFor(long articleId) {
    return pendingDelta(articleId);
  }

  @Transactional
  public Map<Long, Long> flush() {
    Map<Long, Long> flushed = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : blogCache.scanByPrefix(KEY_PREFIX).entrySet()) {
      Long articleId = articleIdFromKey(entry.getKey());
      long delta = parseLong(entry.getValue());
      if (articleId == null || delta <= 0) {
        continue;
      }
      visitRepository.incrementPublishedArticleViewCount(articleId, delta);
      decrementAfterCommit(entry.getKey(), delta);
      flushed.put(articleId, delta);
    }
    return flushed;
  }

  private void decrementAfterCommit(String key, long delta) {
    Runnable decrement = () -> blogCache.incrementByWithTtl(key, -delta, PENDING_TTL);
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      decrement.run();
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            decrement.run();
          }
        });
  }

  private long pendingDelta(long articleId) {
    return Math.max(0L, blogCache.get(key(articleId)).map(this::parseLong).orElse(0L));
  }

  private String key(long articleId) {
    return KEY_PREFIX + articleId;
  }

  private Long articleIdFromKey(String key) {
    if (!key.startsWith(KEY_PREFIX)) {
      return null;
    }
    try {
      return Long.parseLong(key.substring(KEY_PREFIX.length()));
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private long parseLong(String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException exception) {
      return 0;
    }
  }
}
