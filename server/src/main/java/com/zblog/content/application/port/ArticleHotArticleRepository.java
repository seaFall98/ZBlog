package com.zblog.content.application.port;

import java.util.List;
import java.util.Map;

public interface ArticleHotArticleRepository {

  List<Map<String, Object>> hotPublished(int limit);

  List<Map<String, Object>> findPublishedByIds(List<Long> ids);
}
