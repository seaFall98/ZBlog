package com.zblog.notification;

import com.zblog.common.api.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/notifications")
public class NotificationAdminController {

  @GetMapping
  public ApiResponse<Map<String, Object>> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "10") int pageSize) {
    return ApiResponse.ok(
        Map.of("list", List.of(), "total", 0, "page", page, "page_size", pageSize, "unread_count", 0));
  }

  @PutMapping("/{id}/read")
  public ApiResponse<Void> read(@PathVariable long id) {
    return ApiResponse.ok(null);
  }

  @PutMapping("/read-all")
  public ApiResponse<Void> readAll() {
    return ApiResponse.ok(null);
  }
}
