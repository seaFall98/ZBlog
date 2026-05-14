package com.zblog.taxonomy.controller;

import com.zblog.common.api.ApiResponse;
import com.zblog.common.api.PageResponse;
import com.zblog.taxonomy.application.TaxonomyService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CategoryController {

  private final TaxonomyService taxonomyService;

  public CategoryController(TaxonomyService taxonomyService) {
    this.taxonomyService = taxonomyService;
  }

  @GetMapping({"/categories", "/admin/categories"})
  public ApiResponse<PageResponse<Map<String, Object>>> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "50") int pageSize) {
    return ApiResponse.ok(taxonomyService.listCategories(page, pageSize));
  }

  @GetMapping({"/categories/{idOrSlug}", "/admin/categories/{idOrSlug}"})
  public ApiResponse<Map<String, Object>> get(@PathVariable String idOrSlug) {
    return ApiResponse.ok(taxonomyService.getCategory(idOrSlug));
  }

  @PostMapping("/admin/categories")
  public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
    return ApiResponse.ok(taxonomyService.createCategory(request));
  }

  @PutMapping("/admin/categories/{id}")
  public ApiResponse<Map<String, Object>> update(
      @PathVariable long id, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(taxonomyService.updateCategory(id, request));
  }

  @DeleteMapping("/admin/categories/{id}")
  public ApiResponse<Void> delete(@PathVariable long id) {
    taxonomyService.deleteCategory(id);
    return ApiResponse.ok(null);
  }
}
