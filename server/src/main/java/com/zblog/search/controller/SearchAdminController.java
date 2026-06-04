package com.zblog.search.controller;

import com.zblog.common.api.ApiResponse;
import com.zblog.search.application.SearchService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/search")
public class SearchAdminController {

  private final SearchService searchService;

  public SearchAdminController(SearchService searchService) {
    this.searchService = searchService;
  }

  @PostMapping("/reindex")
  public ApiResponse<Map<String, Object>> reindex() {
    return ApiResponse.ok(searchService.reindexAllPublished());
  }

  @GetMapping("/status")
  public ApiResponse<Map<String, Object>> status() {
    return ApiResponse.ok(searchService.status());
  }
}
