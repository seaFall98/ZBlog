package com.zblog.notification;

import com.zblog.common.api.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class NotificationAdminController {

  private final NotificationService notificationService;

  public NotificationAdminController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping({"/admin/notifications", "/notifications"})
  public ApiResponse<Map<String, Object>> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "10") int pageSize) {
    return ApiResponse.ok(notificationService.list(page, pageSize));
  }

  @PutMapping({"/admin/notifications/{id}/read", "/notifications/{id}/read"})
  public ApiResponse<Map<String, Object>> read(@PathVariable long id) {
    return ApiResponse.ok(notificationService.markRead(id));
  }

  @PutMapping({"/admin/notifications/read-all", "/notifications/read-all"})
  public ApiResponse<Map<String, Object>> readAll() {
    return ApiResponse.ok(notificationService.markAllRead());
  }
}
