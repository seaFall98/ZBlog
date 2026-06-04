package com.zblog.content.application.port;

import com.zblog.common.api.PageResponse;
import java.util.Map;

public interface ArticlePublicQueryRepository {

  PageResponse<Map<String, Object>> listPublic(int page, int pageSize, String category, String tag, String year, String month);

  Map<String, Object> getPublicBySlug(String slug);

  String randomPublishedSlug();

  PageResponse<Map<String, Object>> searchPublic(String keyword, int page, int pageSize);
}
