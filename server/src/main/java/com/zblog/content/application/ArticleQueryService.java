package com.zblog.content.application;

import com.zblog.common.api.PageResponse;
import com.zblog.content.application.port.ArticleAdminQueryRepository;
import com.zblog.content.application.port.ArticlePublicQueryRepository;
import com.zblog.stats.application.ArticleViewCountBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ArticleQueryService {

  private final ArticlePublicQueryRepository publicQueryRepository;
  private final ArticleAdminQueryRepository adminQueryRepository;
  private final ArticleViewCountBuffer articleViewCountBuffer;

  public ArticleQueryService(
      ArticlePublicQueryRepository publicQueryRepository,
      ArticleAdminQueryRepository adminQueryRepository,
      ArticleViewCountBuffer articleViewCountBuffer) {
    this.publicQueryRepository = publicQueryRepository;
    this.adminQueryRepository = adminQueryRepository;
    this.articleViewCountBuffer = articleViewCountBuffer;
  }

  public PageResponse<Map<String, Object>> listPublic(
      int page, int pageSize, String category, String tag, String year, String month) {
    return withVisibleViewCounts(publicQueryRepository.listPublic(page, pageSize, category, tag, year, month));
  }

  public Map<String, Object> getPublicBySlug(String slug) {
    return withVisibleViewCount(publicQueryRepository.getPublicBySlug(slug));
  }

  public String randomPublishedSlug() {
    return publicQueryRepository.randomPublishedSlug();
  }

  public PageResponse<Map<String, Object>> searchPublic(String keyword, int page, int pageSize) {
    return withVisibleViewCounts(publicQueryRepository.searchPublic(keyword, page, pageSize));
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

  private PageResponse<Map<String, Object>> withVisibleViewCounts(PageResponse<Map<String, Object>> page) {
    return new PageResponse<>(
        page.list().stream().map(this::withVisibleViewCount).toList(),
        page.total(),
        page.page(),
        page.pageSize());
  }

  private Map<String, Object> withVisibleViewCount(Map<String, Object> article) {
    Map<String, Object> copy = new LinkedHashMap<>(article);
    Object id = copy.get("id");
    Object current = copy.get("view_count");
    if (id instanceof Number articleId && current instanceof Number persisted) {
      long pending = articleViewCountBuffer.pendingDeltaFor(articleId.longValue());
      if (pending > 0) {
        copy.put("view_count", persisted.longValue() + pending);
      }
    }
    return copy;
  }
}
