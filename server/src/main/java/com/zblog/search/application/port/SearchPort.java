package com.zblog.search.application.port;

import com.zblog.common.api.PageResponse;
import java.util.Map;

public interface SearchPort {
  PageResponse<Map<String, Object>> search(String keyword, int page, int pageSize);
}
