package com.zblog.album.controller;

import com.zblog.album.application.AlbumService;
import com.zblog.common.api.ApiResponse;
import com.zblog.common.api.PageResponse;
import java.util.List;
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
public class AlbumController {

  private final AlbumService albumService;

  public AlbumController(AlbumService albumService) {
    this.albumService = albumService;
  }

  @GetMapping("/albums")
  public ApiResponse<PageResponse<Map<String, Object>>> listPublic(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
    return ApiResponse.ok(albumService.listPublic(page, pageSize));
  }

  @GetMapping("/albums/{slug}")
  public ApiResponse<Map<String, Object>> getPublic(@PathVariable String slug) {
    return ApiResponse.ok(albumService.getPublicBySlug(slug));
  }

  @GetMapping("/admin/albums")
  public ApiResponse<PageResponse<Map<String, Object>>> listAdmin(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
      @RequestParam(required = false) String keyword,
      @RequestParam(name = "is_public", required = false) Boolean isPublic) {
    return ApiResponse.ok(albumService.listAdmin(page, pageSize, keyword, isPublic));
  }

  @GetMapping("/admin/albums/{id}")
  public ApiResponse<Map<String, Object>> getAdmin(@PathVariable long id) {
    return ApiResponse.ok(albumService.getAdmin(id));
  }

  @PostMapping("/admin/albums")
  public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
    return ApiResponse.ok(albumService.create(request));
  }

  @PutMapping("/admin/albums/{id}")
  public ApiResponse<Map<String, Object>> update(
      @PathVariable long id, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(albumService.update(id, request));
  }

  @DeleteMapping("/admin/albums/{id}")
  public ApiResponse<Void> delete(@PathVariable long id) {
    albumService.delete(id);
    return ApiResponse.ok(null);
  }

  @PostMapping("/admin/albums/{id}/photos")
  public ApiResponse<Map<String, Object>> addPhoto(
      @PathVariable long id, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(albumService.addPhoto(id, request));
  }

  @PutMapping("/admin/albums/{id}/photos/{photoId}")
  public ApiResponse<Map<String, Object>> updatePhoto(
      @PathVariable long id, @PathVariable long photoId, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(albumService.updatePhoto(id, photoId, request));
  }

  @DeleteMapping("/admin/albums/{id}/photos/{photoId}")
  public ApiResponse<Void> deletePhoto(@PathVariable long id, @PathVariable long photoId) {
    albumService.deletePhoto(id, photoId);
    return ApiResponse.ok(null);
  }

  @PutMapping("/admin/albums/{id}/photos/reorder")
  public ApiResponse<List<Map<String, Object>>> reorderPhotos(
      @PathVariable long id, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(albumService.reorderPhotos(id, request));
  }
}
