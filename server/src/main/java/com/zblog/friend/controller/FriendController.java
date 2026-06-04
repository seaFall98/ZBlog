package com.zblog.friend.controller;

import com.zblog.common.api.ApiResponse;
import com.zblog.common.api.PageResponse;
import com.zblog.friend.application.FriendService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class FriendController {

  private final FriendService friendService;

  public FriendController(FriendService friendService) {
    this.friendService = friendService;
  }

  @GetMapping("/friends")
  public ApiResponse<Map<String, Object>> groupedPublic() {
    return ApiResponse.ok(friendService.groupedPublic());
  }

  @PostMapping("/friends/apply")
  public ApiResponse<Map<String, Object>> apply(@RequestBody Map<String, Object> request) {
    return ApiResponse.ok(friendService.applyFriend(request));
  }

  @GetMapping("/admin/friends/types")
  public ApiResponse<PageResponse<Map<String, Object>>> listTypes(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "50") int pageSize) {
    return ApiResponse.ok(friendService.listTypes(page, pageSize));
  }

  @GetMapping("/admin/friends/types/{id}")
  public ApiResponse<Map<String, Object>> getType(@PathVariable long id) {
    return ApiResponse.ok(friendService.getType(id));
  }

  @PostMapping("/admin/friends/types")
  public ApiResponse<Map<String, Object>> createType(@RequestBody Map<String, Object> request) {
    return ApiResponse.ok(friendService.createType(request));
  }

  @PutMapping("/admin/friends/types/{id}")
  public ApiResponse<Map<String, Object>> updateType(
      @PathVariable long id, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(friendService.updateType(id, request));
  }

  @DeleteMapping("/admin/friends/types/{id}")
  public ApiResponse<Void> deleteType(@PathVariable long id) {
    friendService.deleteType(id);
    return ApiResponse.ok(null);
  }

  @GetMapping("/admin/friends")
  public ApiResponse<PageResponse<Map<String, Object>>> listAdmin(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "50") int pageSize) {
    return ApiResponse.ok(friendService.listAdmin(page, pageSize));
  }

  @GetMapping("/admin/friends/{id}")
  public ApiResponse<Map<String, Object>> getFriend(@PathVariable long id) {
    return ApiResponse.ok(friendService.getFriend(id));
  }

  @PostMapping("/admin/friends")
  public ApiResponse<Map<String, Object>> createFriend(@RequestBody Map<String, Object> request) {
    return ApiResponse.ok(friendService.createFriend(request));
  }

  @PutMapping("/admin/friends/{id}")
  public ApiResponse<Map<String, Object>> updateFriend(
      @PathVariable long id, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(friendService.updateFriend(id, request));
  }

  @DeleteMapping("/admin/friends/{id}")
  public ApiResponse<Void> deleteFriend(@PathVariable long id) {
    friendService.deleteFriend(id);
    return ApiResponse.ok(null);
  }
}
