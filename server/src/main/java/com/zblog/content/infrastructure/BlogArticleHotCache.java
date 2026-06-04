package com.zblog.content.infrastructure;

import com.zblog.cache.BlogCache;
import com.zblog.content.application.port.ArticleHotCache;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BlogArticleHotCache implements ArticleHotCache {

  private final BlogCache blogCache;

  public BlogArticleHotCache(BlogCache blogCache) {
    this.blogCache = blogCache;
  }

  public List<Long> topHotArticles(int limit) {
    return blogCache.topHotArticles(limit);
  }

  public Map<Long, Double> hotArticleScores(List<Long> articleIds) {
    return blogCache.hotArticleScores(articleIds);
  }
}
