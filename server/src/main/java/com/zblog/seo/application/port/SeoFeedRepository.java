package com.zblog.seo.application.port;

import java.time.Instant;
import java.util.List;

public interface SeoFeedRepository {

  List<FeedArticle> publishedFeedArticles();

  record FeedArticle(
      String slug, String title, String summary, String coverUrl, Instant publishedAt, Instant updatedAt) {}
}
