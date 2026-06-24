package com.zblog.stats.application;

import com.zblog.stats.infrastructure.mybatis.DailyStatsArchiveMapper;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyStatsArchiveService {

  private final DailyStatsArchiveMapper mapper;

  public DailyStatsArchiveService(DailyStatsArchiveMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional
  public Map<String, Object> archive(LocalDate statDate) {
    mapper.deleteArticleDaily(statDate);
    mapper.deleteSiteDaily(statDate);
    mapper.insertSiteDaily(statDate);
    mapper.insertArticleDaily(statDate);
    return Map.of(
        "stat_date", statDate.toString(),
        "site_rows", 1,
        "article_rows", mapper.countArticleDaily(statDate));
  }
}
