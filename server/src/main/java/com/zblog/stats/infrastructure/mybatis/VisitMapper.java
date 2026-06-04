package com.zblog.stats.infrastructure.mybatis;

import com.zblog.stats.domain.VisitEventInput;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VisitMapper {

  int insertVisit(VisitEventInput input);

  int incrementPublishedArticleViewCount(@Param("articleId") long articleId);

  Long articleViewCount(@Param("articleId") long articleId);
}
