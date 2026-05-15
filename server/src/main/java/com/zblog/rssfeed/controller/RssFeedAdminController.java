package com.zblog.rssfeed.controller;

import com.zblog.common.api.ApiResponse;
import com.zblog.rssfeed.application.RssFeedAdminService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/rssfeed")
public class RssFeedAdminController {

  private final RssFeedAdminService rssFeedAdminService;

  public RssFeedAdminController(RssFeedAdminService rssFeedAdminService) {
    this.rssFeedAdminService = rssFeedAdminService;
  }

  @GetMapping
  public ApiResponse<Map<String, Object>> list(@RequestParam Map<String, String> params) {
    return ApiResponse.ok(rssFeedAdminService.listAdmin(params));
  }

  @PutMapping("/{id}/read")
  public ApiResponse<Void> markRead(@PathVariable long id) {
    rssFeedAdminService.markRead(id);
    return ApiResponse.ok(null);
  }

  @PutMapping("/read-all")
  public ApiResponse<Map<String, Object>> markAllRead() {
    return ApiResponse.ok(rssFeedAdminService.markAllRead());
  }
}
