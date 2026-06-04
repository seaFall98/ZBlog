package com.zblog.content.application.port;

import com.zblog.common.api.PageResponse;
import java.util.List;
import java.util.Map;

public interface ArticleAdminQueryRepository {

  PageResponse<Map<String, Object>> listAdmin(
      int page,
      int pageSize,
      String keyword,
      Boolean published,
      Long categoryId,
      List<Long> tagIds,
      String location,
      Boolean top,
      Boolean essence,
      Boolean outdated,
      String startTime,
      String endTime);

  Map<String, Object> getAdmin(long id);
}
