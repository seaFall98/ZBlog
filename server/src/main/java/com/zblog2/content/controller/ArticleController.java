package com.zblog2.content.controller;

import com.zblog2.common.api.ApiResponse;
import com.zblog2.common.api.PageResponse;
import com.zblog2.content.application.ArticleService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ArticleController {

  private final ArticleService articleService;

  public ArticleController(ArticleService articleService) {
    this.articleService = articleService;
  }

  @GetMapping("/articles")
  public ApiResponse<PageResponse<Map<String, Object>>> listPublic(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String tag,
      @RequestParam(required = false) String year,
      @RequestParam(required = false) String month) {
    return ApiResponse.ok(articleService.listPublic(page, pageSize, category, tag, year, month));
  }

  @GetMapping("/articles/random")
  public ApiResponse<String> random() {
    return ApiResponse.ok(articleService.randomPublishedSlug());
  }

  @GetMapping("/articles/{slug}")
  public ApiResponse<Map<String, Object>> getPublic(@PathVariable String slug) {
    return ApiResponse.ok(articleService.getPublicBySlug(slug));
  }

  @GetMapping("/admin/articles")
  public ApiResponse<PageResponse<Map<String, Object>>> listAdmin(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
      @RequestParam(required = false) String keyword,
      @RequestParam(name = "is_publish", required = false) Boolean published) {
    return ApiResponse.ok(articleService.listAdmin(page, pageSize, keyword, published));
  }

  @GetMapping("/admin/articles/{id}")
  public ApiResponse<Map<String, Object>> getAdmin(@PathVariable long id) {
    return ApiResponse.ok(articleService.getAdmin(id));
  }

  @PostMapping("/admin/articles")
  public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
    return ApiResponse.ok(articleService.create(request));
  }

  @PutMapping("/admin/articles/{id}")
  public ApiResponse<Map<String, Object>> update(
      @PathVariable long id, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(articleService.update(id, request));
  }

  @PostMapping("/admin/articles/{id}/publish")
  public ApiResponse<Map<String, Object>> publish(@PathVariable long id) {
    return ApiResponse.ok(articleService.publish(id));
  }

  @PostMapping("/admin/articles/{id}/unpublish")
  public ApiResponse<Map<String, Object>> unpublish(@PathVariable long id) {
    return ApiResponse.ok(articleService.unpublish(id));
  }

  @DeleteMapping("/admin/articles/{id}")
  public ApiResponse<Void> delete(@PathVariable long id) {
    articleService.delete(id);
    return ApiResponse.ok(null);
  }
}
