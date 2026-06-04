package com.zblog.content.application;

import com.zblog.common.api.PageResponse;
import com.zblog.content.application.port.ArticleAdminQueryRepository;
import com.zblog.content.application.port.ArticlePublicQueryRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ArticleQueryService {

  private final ArticlePublicQueryRepository publicQueryRepository;
  private final ArticleAdminQueryRepository adminQueryRepository;

  public ArticleQueryService(
      ArticlePublicQueryRepository publicQueryRepository,
      ArticleAdminQueryRepository adminQueryRepository) {
    this.publicQueryRepository = publicQueryRepository;
    this.adminQueryRepository = adminQueryRepository;
  }

  public PageResponse<Map<String, Object>> listPublic(
      int page, int pageSize, String category, String tag, String year, String month) {
    return publicQueryRepository.listPublic(page, pageSize, category, tag, year, month);
  }

  public Map<String, Object> getPublicBySlug(String slug) {
    return publicQueryRepository.getPublicBySlug(slug);
  }

  public String randomPublishedSlug() {
    return publicQueryRepository.randomPublishedSlug();
  }

  public PageResponse<Map<String, Object>> searchPublic(String keyword, int page, int pageSize) {
    return publicQueryRepository.searchPublic(keyword, page, pageSize);
  }

  public PageResponse<Map<String, Object>> listAdmin(
      int page, int pageSize, String keyword, Boolean published) {
    return adminQueryRepository.listAdmin(
        page, pageSize, keyword, published, null, List.of(), null, null, null, null, null, null);
  }

  public PageResponse<Map<String, Object>> listAdmin(
      int page,
      int pageSize,
      String keyword,
      Boolean published,
      Long categoryId,
      List<Long> tagIds,
      String location,
      Boolean top,
      Boolean essence,
      Boolean outdated,
      String startTime,
      String endTime) {
    return adminQueryRepository.listAdmin(
        page,
        pageSize,
        keyword,
        published,
        categoryId,
        tagIds == null ? List.of() : tagIds,
        location,
        top,
        essence,
        outdated,
        startTime,
        endTime);
  }

  public Map<String, Object> getAdmin(long id) {
    return adminQueryRepository.getAdmin(id);
  }
}
