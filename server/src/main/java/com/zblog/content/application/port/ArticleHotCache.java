package com.zblog.content.application.port;

import java.util.List;
import java.util.Map;

public interface ArticleHotCache {

  List<Long> topHotArticles(int limit);

  Map<Long, Double> hotArticleScores(List<Long> articleIds);
}
