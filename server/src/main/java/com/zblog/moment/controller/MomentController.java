package com.zblog.moment.controller;

import com.zblog.common.api.ApiResponse;
import com.zblog.common.api.PageResponse;
import com.zblog.moment.application.MomentService;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MomentController {

  private final MomentService momentService;

  public MomentController(MomentService momentService) {
    this.momentService = momentService;
  }

  @GetMapping("/moments")
  public ApiResponse<PageResponse<Map<String, Object>>> listPublic(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "30") int pageSize) {
    return ApiResponse.ok(momentService.listPublic(page, pageSize));
  }

  @GetMapping("/admin/moments")
  public ApiResponse<PageResponse<Map<String, Object>>> listAdmin(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String tags,
      @RequestParam(required = false) String location,
      @RequestParam(name = "is_publish", required = false) Boolean isPublish,
      @RequestParam(name = "has_images", required = false) Boolean hasImages,
      @RequestParam(name = "has_video", required = false) Boolean hasVideo,
      @RequestParam(name = "has_audio", required = false) Boolean hasAudio,
      @RequestParam(name = "has_music", required = false) Boolean hasMusic,
      @RequestParam(name = "has_link", required = false) Boolean hasLink,
      @RequestParam(name = "start_time", required = false) String startTime,
      @RequestParam(name = "end_time", required = false) String endTime) {
    return ApiResponse.ok(
        momentService.listAdmin(
            page,
            pageSize,
            keyword,
            tags,
            location,
            isPublish,
            hasImages,
            hasVideo,
            hasAudio,
            hasMusic,
            hasLink,
            startTime,
            endTime));
  }

  @GetMapping("/admin/moments/{id}")
  public ApiResponse<Map<String, Object>> get(@PathVariable long id) {
    return ApiResponse.ok(momentService.get(id));
  }

  @PostMapping("/admin/moments")
  public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
    return ApiResponse.ok(momentService.create(request));
  }

  @PutMapping("/admin/moments/{id}")
  public ApiResponse<Map<String, Object>> update(
      @PathVariable long id, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(momentService.update(id, request));
  }

  @DeleteMapping("/admin/moments/{id}")
  public ApiResponse<Void> delete(@PathVariable long id) {
    momentService.delete(id);
    return ApiResponse.ok(null);
  }
}
