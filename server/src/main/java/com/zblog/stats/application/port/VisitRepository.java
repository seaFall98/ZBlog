package com.zblog.stats.application.port;

import com.zblog.stats.domain.VisitEventInput;

public interface VisitRepository {

  void save(VisitEventInput input);

  void incrementPublishedArticleViewCount(long articleId);

  Long articleViewCount(long articleId);
}
