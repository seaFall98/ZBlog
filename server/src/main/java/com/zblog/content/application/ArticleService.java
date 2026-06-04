package com.zblog.content.application;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.common.util.Slugify;
import com.zblog.content.application.MarkdownRenderer.RenderedContent;
import com.zblog.content.application.port.ArticleEventPublisher;
import com.zblog.content.application.port.ArticleAdminQueryRepository;
import com.zblog.content.application.port.ArticleCommandRepository;
import com.zblog.content.application.port.ArticleSearchProjectionRepository;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleService {

  private final ArticleCommandRepository articleRepository;
  private final ArticleAdminQueryRepository adminQueryRepository;
  private final ArticleSearchProjectionRepository searchProjectionRepository;
  private final ArticleEventPublisher articleEventPublisher;
  private final MarkdownRenderer markdownRenderer;
  private final ArticleQueryService articleQueryService;
  private final ArticleHotRankingService articleHotRankingService;
  private final ArticleImportService articleImportService;
  private final ArticleExportService articleExportService;

  public ArticleService(
      ArticleCommandRepository articleRepository,
      ArticleAdminQueryRepository adminQueryRepository,
      ArticleSearchProjectionRepository searchProjectionRepository,
      ArticleEventPublisher articleEventPublisher,
      MarkdownRenderer markdownRenderer,
      ArticleQueryService articleQueryService,
      ArticleHotRankingService articleHotRankingService,
      ArticleImportService articleImportService,
      ArticleExportService articleExportService) {
    this.articleRepository = articleRepository;
    this.adminQueryRepository = adminQueryRepository;
    this.searchProjectionRepository = searchProjectionRepository;
    this.articleEventPublisher = articleEventPublisher;
    this.markdownRenderer = markdownRenderer;
    this.articleQueryService = articleQueryService;
    this.articleHotRankingService = articleHotRankingService;
    this.articleImportService = articleImportService;
    this.articleExportService = articleExportService;
  }

  public PageResponse<Map<String, Object>> listPublic(
      int page, int pageSize, String category, String tag, String year, String month) {
    return articleQueryService.listPublic(page, pageSize, category, tag, year, month);
  }

  public Map<String, Object> getPublicBySlug(String slug) {
    return articleQueryService.getPublicBySlug(slug);
  }

  public String randomPublishedSlug() {
    return articleQueryService.randomPublishedSlug();
  }

  public PageResponse<Map<String, Object>> searchPublic(String keyword, int page, int pageSize) {
    return articleQueryService.searchPublic(keyword, page, pageSize);
  }

  public PageResponse<Map<String, Object>> hotArticles(int limit, String type) {
    return articleHotRankingService.hotArticles(limit, type);
  }

  public PageResponse<Map<String, Object>> listAdmin(
      int page, int pageSize, String keyword, Boolean published) {
    return articleQueryService.listAdmin(page, pageSize, keyword, published);
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
    return articleQueryService.listAdmin(
        page,
        pageSize,
        keyword,
        published,
        categoryId,
        tagIds,
        location,
        top,
        essence,
        outdated,
        startTime,
        endTime);
  }

  public Map<String, Object> getAdmin(long id) {
    return articleQueryService.getAdmin(id);
  }

  @Transactional
  public Map<String, Object> create(Map<String, Object> request) {
    String title = text(request, "title");
    String slug = textOrDefault(request, "slug", Slugify.from(title));
    String markdown = text(request, "content");
    RenderedContent rendered = markdownRenderer.render(markdown);
    Map<String, Object> created;
    try {
      created =
          articleRepository.create(
              title,
              slug,
              markdown,
              rendered.html(),
              rendered.text(),
              textOrDefault(request, "summary", ""),
              nullableText(request, "cover"),
              nullableLong(request, "category_id"),
              tagIds(request),
              nullableText(request, "location"),
              bool(request, "is_top"),
              bool(request, "is_essence"),
              bool(request, "is_outdated"));
    } catch (DuplicateKeyException exception) {
      throw new BusinessException(40901, "Article slug already exists: " + slug, HttpStatus.CONFLICT);
    }
    if (bool(request, "is_publish")) {
      return publish(((Number) created.get("id")).longValue());
    }
    return created;
  }

  @Transactional
  public Map<String, Object> update(long id, Map<String, Object> request) {
    Map<String, Object> existing = adminQueryRepository.getAdmin(id);
    boolean wasPublished = Boolean.TRUE.equals(existing.get("is_publish"));
    String title = textOrDefault(request, "title", existing.get("title").toString());
    String slug = textOrDefault(request, "slug", existing.get("slug").toString());
    String markdown = textOrDefault(request, "content", value(existing, "content_markdown"));
    RenderedContent rendered = markdownRenderer.render(markdown);
    Map<String, Object> updated;
    try {
      updated =
          articleRepository.update(
              id,
              title,
              slug,
              markdown,
              rendered.html(),
              rendered.text(),
              textOrDefault(request, "summary", value(existing, "summary")),
              nullableTextOrDefault(request, "cover", value(existing, "cover")),
              nullableLong(request, "category_id"),
              tagIds(request),
              nullableTextOrDefault(request, "location", value(existing, "location")),
              boolOrDefault(request, "is_top", (Boolean) existing.get("is_top")),
              boolOrDefault(request, "is_essence", (Boolean) existing.get("is_essence")),
              boolOrDefault(request, "is_outdated", (Boolean) existing.get("is_outdated")));
    } catch (DuplicateKeyException exception) {
      throw new BusinessException(40901, "Article slug already exists: " + slug, HttpStatus.CONFLICT);
    }
    if (request.containsKey("is_publish")) {
      return bool(request, "is_publish") ? publish(id) : unpublish(id);
    }
    if (wasPublished) {
      emitArticleSearchUpsert(id);
    }
    return updated;
  }

  @Transactional
  public Map<String, Object> publish(long id) {
    Map<String, Object> existing = adminQueryRepository.getAdmin(id);
    boolean alreadyPublished = Boolean.TRUE.equals(existing.get("is_publish"));
    Map<String, Object> published = articleRepository.publish(id);
    if (!alreadyPublished) {
      articleEventPublisher.articlePublished(published);
      emitArticleSearchUpsert(id);
    }
    return published;
  }

  @Transactional
  public Map<String, Object> unpublish(long id) {
    Map<String, Object> existing = adminQueryRepository.getAdmin(id);
    boolean wasPublished = Boolean.TRUE.equals(existing.get("is_publish"));
    Map<String, Object> unpublished = articleRepository.unpublish(id);
    if (wasPublished) {
      articleEventPublisher.articleSearchDelete(id);
    }
    return unpublished;
  }

  @Transactional
  public void delete(long id) {
    Map<String, Object> existing = adminQueryRepository.getAdmin(id);
    boolean wasPublished = Boolean.TRUE.equals(existing.get("is_publish"));
    articleRepository.delete(id);
    if (wasPublished) {
      articleEventPublisher.articleSearchDelete(id);
    }
  }

  private void emitArticleSearchUpsert(long id) {
    searchProjectionRepository.publishedSearchProjection(id).ifPresent(articleEventPublisher::articleSearchUpsert);
  }

  public Map<String, Object> importArticles(String sourceType, List<ImportedArticleFile> files) {
    return articleImportService.importArticles(sourceType, files);
  }

  public Map<String, Object> exportToWeChat(long id) {
    return articleExportService.exportToWeChat(id);
  }

  public byte[] downloadMarkdownZip(long id) {
    return articleExportService.downloadMarkdownZip(id);
  }

  private String text(Map<String, Object> request, String key) {
    Object value = request.get(key);
    return value == null ? "" : value.toString().trim();
  }

  private String textOrDefault(Map<String, Object> request, String key, String defaultValue) {
    String value = text(request, key);
    return value.isBlank() ? defaultValue : value;
  }

  private String nullableText(Map<String, Object> request, String key) {
    String value = text(request, key);
    return value.isBlank() ? null : value;
  }

  private String nullableTextOrDefault(Map<String, Object> request, String key, String defaultValue) {
    return request.containsKey(key) ? nullableText(request, key) : defaultValue;
  }

  private Long nullableLong(Map<String, Object> request, String key) {
    Object value = request.get(key);
    return value instanceof Number number ? number.longValue() : null;
  }

  private boolean bool(Map<String, Object> request, String key) {
    Object value = request.get(key);
    return value instanceof Boolean bool && bool;
  }

  private boolean boolOrDefault(Map<String, Object> request, String key, boolean defaultValue) {
    return request.containsKey(key) ? bool(request, key) : defaultValue;
  }

  private List<Long> tagIds(Map<String, Object> request) {
    Object value = request.get("tag_ids");
    if (!(value instanceof List<?> list)) {
      return List.of();
    }
    return list.stream()
        .filter(Number.class::isInstance)
        .map(Number.class::cast)
        .map(Number::longValue)
        .toList();
  }

  private String value(Map<String, Object> map, String key) {
    Object value = map.get(key);
    return value == null ? "" : value.toString();
  }
}
