package com.zblog.content.controller;

import com.zblog.common.api.ApiResponse;
import com.zblog.common.api.PageResponse;
import com.zblog.content.application.ArticleService;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

  @GetMapping("/articles/search")
  public ApiResponse<PageResponse<Map<String, Object>>> search(
      @RequestParam(defaultValue = "") String keyword,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "10") int pageSize) {
    return ApiResponse.ok(articleService.searchPublic(keyword, page, pageSize));
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

  @PostMapping(value = "/admin/articles/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<Map<String, Object>> importArticles(
      @RequestParam(name = "source_type", defaultValue = "markdown") String sourceType,
      @RequestParam(name = "files") MultipartFile[] files) {
    return ApiResponse.ok(articleService.importArticles(sourceType, files));
  }

  @PostMapping("/admin/articles/{id}/wechat/export")
  public ApiResponse<Map<String, Object>> exportToWeChat(@PathVariable long id) {
    return ApiResponse.ok(articleService.exportToWeChat(id));
  }

  @GetMapping("/admin/articles/{id}/download/zip")
  public ResponseEntity<byte[]> downloadZip(@PathVariable long id) {
    byte[] zip = articleService.downloadMarkdownZip(id);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename("article-" + id + ".zip").build().toString())
        .body(zip);
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
