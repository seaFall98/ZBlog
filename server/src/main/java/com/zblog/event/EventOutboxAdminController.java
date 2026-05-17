package com.zblog.event;

import com.zblog.common.api.ApiResponse;
import com.zblog.common.api.PageResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/outbox")
public class EventOutboxAdminController {

  private final EventOutboxService eventOutboxService;

  public EventOutboxAdminController(EventOutboxService eventOutboxService) {
    this.eventOutboxService = eventOutboxService;
  }

  @GetMapping
  public ApiResponse<PageResponse<Map<String, Object>>> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
      @RequestParam(required = false) String status) {
    return ApiResponse.ok(eventOutboxService.list(page, pageSize, status));
  }

  @PostMapping("/publish-pending")
  public ApiResponse<Map<String, Object>> publishPending() {
    return ApiResponse.ok(eventOutboxService.publishPending());
  }
}
