package com.zblog.stats.application.port;

import com.zblog.stats.domain.VisitEventInput;

public interface VisitRepository {

  void save(VisitEventInput input);

  default void incrementPublishedArticleViewCount(long articleId) {
    incrementPublishedArticleViewCount(articleId, 1);
  }

  void incrementPublishedArticleViewCount(long articleId, long delta);

  Long articleViewCount(long articleId);
}
