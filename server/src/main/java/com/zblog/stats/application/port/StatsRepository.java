package com.zblog.stats.application.port;

import com.zblog.common.api.PageResponse;
import com.zblog.stats.application.VisitQuery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface StatsRepository {

  long countPublishedArticles();

  long countVisibleComments();

  long countVisibleFriends();

  long countPublishedCategories();

  long countPublishedTags();

  long countPageviewEvents();

  long countPublishedWords();

  long countVisitors();

  long countOnlineVisitors(LocalDateTime since);

  long countMoments();

  long countAlbumPhotos();

  long countGuestbookMessages();

  long countVisitors(LocalDate start, LocalDate end);

  long countPageviews(LocalDate start, LocalDate end);

  List<Map<String, Object>> publishedArticleDates();

  List<Map<String, Object>> archiveStats();

  long countActiveUsers();

  long countTodayComments();

  long countTodayUsers();

  List<Map<String, Object>> dailyVisitGroups(LocalDate start, LocalDate end);

  List<Map<String, Object>> monthlyVisitRows(LocalDate start, LocalDate end);

  List<Map<String, Object>> categoryStats();

  List<Map<String, Object>> tagStats();

  PageResponse<Map<String, Object>> visits(VisitQuery query);
}
