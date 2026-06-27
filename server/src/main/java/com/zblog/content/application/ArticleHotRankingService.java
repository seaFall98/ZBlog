package com.zblog.content.application;

import com.zblog.common.api.PageResponse;
import com.zblog.content.application.port.ArticleHotCache;
import com.zblog.content.application.port.ArticleHotArticleRepository;
import com.zblog.stats.application.ArticleViewCountBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ArticleHotRankingService {

  private final ArticleHotArticleRepository articleRepository;
  private final ArticleHotCache articleHotCache;
  private final ArticleViewCountBuffer articleViewCountBuffer;

  public ArticleHotRankingService(
      ArticleHotArticleRepository articleRepository,
      ArticleHotCache articleHotCache,
      ArticleViewCountBuffer articleViewCountBuffer) {
    this.articleRepository = articleRepository;
    this.articleHotCache = articleHotCache;
    this.articleViewCountBuffer = articleViewCountBuffer;
  }

  public PageResponse<Map<String, Object>> hotArticles(int limit, String type) {
    int resolvedLimit = Math.max(1, Math.min(limit, 20));
    if ("total".equalsIgnoreCase(type)) {
      List<Map<String, Object>> totalArticles =
          articleRepository.hotPublished(resolvedLimit).stream().map(this::withVisibleViewCount).toList();
      return new PageResponse<>(totalArticles, totalArticles.size(), 1, resolvedLimit);
    }

    List<Long> hotIds = articleHotCache.topHotArticles(resolvedLimit);
    List<Map<String, Object>> articles =
        hotIds.isEmpty() ? List.of() : articleRepository.findPublishedByIds(hotIds).stream().map(this::withVisibleViewCount).toList();
    Map<Long, Double> scores = articleHotCache.hotArticleScores(hotIds);
    articles = articles.stream().map(article -> withHotScore(article, scores.get(id(article)))).toList();
    if (articles.isEmpty()) {
      articles =
          articleRepository.hotPublished(resolvedLimit).stream()
              .map(this::withVisibleViewCount)
              .map(article -> withHotScore(article, (double) viewCount(article)))
              .toList();
    }
    return new PageResponse<>(articles, articles.size(), 1, resolvedLimit);
  }

  private long id(Map<String, Object> article) {
    return ((Number) article.get("id")).longValue();
  }

  private long viewCount(Map<String, Object> article) {
    Object value = article.get("view_count");
    return value instanceof Number number ? number.longValue() : 0;
  }

  private Map<String, Object> withHotScore(Map<String, Object> article, Double score) {
    Map<String, Object> copy = new LinkedHashMap<>(article);
    copy.put("hot_score", score == null ? 0 : score);
    return copy;
  }

  private Map<String, Object> withVisibleViewCount(Map<String, Object> article) {
    Map<String, Object> copy = new LinkedHashMap<>(article);
    long pending = articleViewCountBuffer.pendingDeltaFor(id(copy));
    if (pending > 0) {
      copy.put("view_count", viewCount(copy) + pending);
    }
    return copy;
  }
}
