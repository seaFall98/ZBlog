package com.zblog2.taxonomy.controller;

import com.zblog2.common.api.ApiResponse;
import com.zblog2.common.api.PageResponse;
import com.zblog2.taxonomy.application.TaxonomyService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class TagController {

  private final TaxonomyService taxonomyService;

  public TagController(TaxonomyService taxonomyService) {
    this.taxonomyService = taxonomyService;
  }

  @GetMapping({"/tags", "/admin/tags"})
  public ApiResponse<PageResponse<Map<String, Object>>> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "50") int pageSize) {
    return ApiResponse.ok(taxonomyService.listTags(page, pageSize));
  }

  @GetMapping({"/tags/{idOrSlug}", "/admin/tags/{idOrSlug}"})
  public ApiResponse<Map<String, Object>> get(@PathVariable String idOrSlug) {
    return ApiResponse.ok(taxonomyService.getTag(idOrSlug));
  }

  @PostMapping("/admin/tags")
  public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
    return ApiResponse.ok(taxonomyService.createTag(request));
  }

  @PutMapping("/admin/tags/{id}")
  public ApiResponse<Map<String, Object>> update(
      @PathVariable long id, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(taxonomyService.updateTag(id, request));
  }

  @DeleteMapping("/admin/tags/{id}")
  public ApiResponse<Void> delete(@PathVariable long id) {
    taxonomyService.deleteTag(id);
    return ApiResponse.ok(null);
  }
}
