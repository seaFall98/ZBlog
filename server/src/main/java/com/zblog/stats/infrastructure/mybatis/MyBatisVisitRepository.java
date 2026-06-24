package com.zblog.stats.infrastructure.mybatis;

import com.zblog.stats.application.port.VisitRepository;
import com.zblog.stats.domain.VisitEventInput;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisVisitRepository implements VisitRepository {

  private final VisitMapper visitMapper;

  public MyBatisVisitRepository(VisitMapper visitMapper) {
    this.visitMapper = visitMapper;
  }

  public void save(VisitEventInput input) {
    visitMapper.insertVisit(input);
  }

  public void incrementPublishedArticleViewCount(long articleId, long delta) {
    visitMapper.incrementPublishedArticleViewCount(articleId, delta);
  }

  public Long articleViewCount(long articleId) {
    return visitMapper.articleViewCount(articleId);
  }
}
