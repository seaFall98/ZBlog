package com.zblog.stats.infrastructure.mybatis;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StatsMapper {

  long countPublishedArticles();

  long countVisibleComments();

  long countVisibleFriends();

  long countPublishedCategories();

  long countPublishedTags();

  long countPageviewEvents();

  long countPublishedWords();

  long countVisitors();

  long countOnlineVisitors(@Param("since") LocalDateTime since);

  long countMoments();

  long countAlbumPhotos();

  long countGuestbookMessages();

  long countVisitorsBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

  long countPageviewsBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

  List<Map<String, Object>> publishedArticleDates();

  long countActiveUsers();

  long countTodayComments();

  long countTodayUsers();

  List<Map<String, Object>> dailyVisitGroups(@Param("start") LocalDate start, @Param("end") LocalDate end);

  List<Map<String, Object>> monthlyVisitRows(@Param("start") LocalDate start, @Param("end") LocalDate end);

  List<Map<String, Object>> categoryStats();

  List<Map<String, Object>> tagStats();

  List<Map<String, Object>> visitRows(
      @Param("keyword") String keyword,
      @Param("visitorId") String visitorId,
      @Param("ip") String ip,
      @Param("excludeIps") List<String> excludeIps,
      @Param("startInclusive") LocalDateTime startInclusive,
      @Param("endExclusive") LocalDateTime endExclusive);
}
