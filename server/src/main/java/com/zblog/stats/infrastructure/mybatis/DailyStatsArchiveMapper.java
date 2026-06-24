package com.zblog.stats.infrastructure.mybatis;

import java.time.LocalDate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DailyStatsArchiveMapper {

  void deleteSiteDaily(@Param("statDate") LocalDate statDate);

  void deleteArticleDaily(@Param("statDate") LocalDate statDate);

  void insertSiteDaily(@Param("statDate") LocalDate statDate);

  void insertArticleDaily(@Param("statDate") LocalDate statDate);

  long countArticleDaily(@Param("statDate") LocalDate statDate);
}
