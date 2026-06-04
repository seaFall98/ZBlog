package com.zblog.stats.controller;

import com.zblog.common.api.ApiResponse;
import com.zblog.common.api.PageResponse;
import com.zblog.stats.application.StatsService;
import com.zblog.stats.application.VisitCollectionService;
import com.zblog.stats.domain.VisitRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class StatsController {

  private final StatsService statsService;
  private final VisitCollectionService visitCollectionService;

  public StatsController(StatsService statsService, VisitCollectionService visitCollectionService) {
    this.statsService = statsService;
    this.visitCollectionService = visitCollectionService;
  }

  @PostMapping("/collect")
  public ApiResponse<Map<String, Object>> collect(
      @RequestBody Map<String, Object> payload, HttpServletRequest request) {
    return ApiResponse.ok(visitCollectionService.collect(payload, requestContext(request)));
  }

  @GetMapping("/stats/site")
  public ApiResponse<Map<String, Object>> siteStats() {
    return ApiResponse.ok(statsService.siteStats());
  }

  @GetMapping("/stats/archives")
  public ApiResponse<Map<String, Object>> archiveStats() {
    return ApiResponse.ok(statsService.archiveStats());
  }

  @GetMapping("/admin/stats/dashboard")
  public ApiResponse<Map<String, Object>> dashboard() {
    return ApiResponse.ok(statsService.dashboard());
  }

  @GetMapping("/admin/stats/trend")
  public ApiResponse<List<Map<String, Object>>> trend(
      @RequestParam(name = "start_date", required = false) String startDate,
      @RequestParam(name = "end_date", required = false) String endDate,
      @RequestParam(defaultValue = "daily") String type) {
    return ApiResponse.ok(statsService.trend(startDate, endDate, type));
  }

  @GetMapping("/admin/stats/category")
  public ApiResponse<List<Map<String, Object>>> categoryStats() {
    return ApiResponse.ok(statsService.categoryStats());
  }

  @GetMapping("/admin/stats/tag")
  public ApiResponse<List<Map<String, Object>>> tagStats() {
    return ApiResponse.ok(statsService.tagStats());
  }

  @GetMapping("/admin/stats/contribution")
  public ApiResponse<List<Map<String, Object>>> contribution(
      @RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month) {
    return ApiResponse.ok(statsService.contribution(year, month));
  }

  private VisitRequestContext requestContext(HttpServletRequest request) {
    return new VisitRequestContext(clientIp(request), header(request, "User-Agent"));
  }

  private String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr() == null ? "" : request.getRemoteAddr();
  }

  private String header(HttpServletRequest request, String name) {
    String value = request.getHeader(name);
    return value == null ? "" : value;
  }

  @GetMapping("/admin/stats/visits")
  public ApiResponse<PageResponse<Map<String, Object>>> visits(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
      @RequestParam(required = false) String keyword,
      @RequestParam(name = "visitor_id", required = false) String visitorId,
      @RequestParam(required = false) String ip,
      @RequestParam(name = "exclude_ips", required = false) String excludeIps,
      @RequestParam(required = false) String location,
      @RequestParam(required = false) String browser,
      @RequestParam(required = false) String os,
      @RequestParam(name = "start_time", required = false) String startTime,
      @RequestParam(name = "end_time", required = false) String endTime) {
    return ApiResponse.ok(
        statsService.visits(
            page, pageSize, keyword, visitorId, ip, excludeIps, location, browser, os, startTime, endTime));
  }
}
