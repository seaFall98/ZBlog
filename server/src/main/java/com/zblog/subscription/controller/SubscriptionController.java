package com.zblog.subscription.controller;

import com.zblog.common.api.ApiResponse;
import com.zblog.common.api.PageResponse;
import com.zblog.subscription.application.SubscriptionService;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SubscriptionController {

  private final SubscriptionService subscriptionService;

  public SubscriptionController(SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  @PostMapping("/subscribe")
  public ApiResponse<Map<String, Object>> subscribe(@RequestBody Map<String, Object> request) {
    return ApiResponse.ok(subscriptionService.subscribe(request));
  }

  @GetMapping("/subscribe/unsubscribe")
  public ApiResponse<Map<String, Object>> unsubscribe(@RequestParam String token) {
    return ApiResponse.ok(subscriptionService.unsubscribe(token));
  }

  @GetMapping("/subscribe/confirm")
  public ApiResponse<Map<String, Object>> confirm(@RequestParam String token) {
    return ApiResponse.ok(subscriptionService.confirm(token));
  }

  @GetMapping("/admin/subscribers")
  public ApiResponse<PageResponse<Map<String, Object>>> listAdmin(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status) {
    return ApiResponse.ok(subscriptionService.listAdmin(page, pageSize, keyword, status));
  }

  @DeleteMapping("/admin/subscribers/{id}")
  public ApiResponse<Void> deleteAdmin(@PathVariable long id) {
    subscriptionService.delete(id);
    return ApiResponse.ok(null);
  }
}
