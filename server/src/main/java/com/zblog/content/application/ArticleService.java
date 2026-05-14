package com.zblog.content.application;

import com.zblog.common.api.PageResponse;
import com.zblog.common.util.Slugify;
import com.zblog.content.application.MarkdownRenderer.RenderedContent;
import com.zblog.content.infrastructure.JdbcArticleRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ArticleService {

  private final JdbcArticleRepository articleRepository;
  private final MarkdownRenderer markdownRenderer;

  public ArticleService(JdbcArticleRepository articleRepository, MarkdownRenderer markdownRenderer) {
    this.articleRepository = articleRepository;
    this.markdownRenderer = markdownRenderer;
  }

  public PageResponse<Map<String, Object>> listPublic(
      int page, int pageSize, String category, String tag, String year, String month) {
    return articleRepository.listPublic(page, pageSize, category, tag, year, month);
  }

  public Map<String, Object> getPublicBySlug(String slug) {
    return articleRepository.getPublicBySlug(slug);
  }

  public String randomPublishedSlug() {
    return articleRepository.randomPublishedSlug();
  }

  public PageResponse<Map<String, Object>> searchPublic(String keyword, int page, int pageSize) {
    return articleRepository.searchPublic(keyword, page, pageSize);
  }

  public PageResponse<Map<String, Object>> listAdmin(
      int page, int pageSize, String keyword, Boolean published) {
    return articleRepository.listAdmin(page, pageSize, keyword, published);
  }

  public Map<String, Object> getAdmin(long id) {
    return articleRepository.getAdmin(id);
  }

  public Map<String, Object> create(Map<String, Object> request) {
    String title = text(request, "title");
    String slug = textOrDefault(request, "slug", Slugify.from(title));
    String markdown = text(request, "content");
    RenderedContent rendered = markdownRenderer.render(markdown);
    return articleRepository.create(
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
  }

  public Map<String, Object> update(long id, Map<String, Object> request) {
    Map<String, Object> existing = articleRepository.getAdmin(id);
    String title = textOrDefault(request, "title", existing.get("title").toString());
    String slug = textOrDefault(request, "slug", existing.get("slug").toString());
    String markdown = textOrDefault(request, "content", existing.get("content").toString());
    RenderedContent rendered = markdownRenderer.render(markdown);
    return articleRepository.update(
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
  }

  public Map<String, Object> publish(long id) {
    return articleRepository.publish(id);
  }

  public Map<String, Object> unpublish(long id) {
    return articleRepository.unpublish(id);
  }

  public void delete(long id) {
    articleRepository.delete(id);
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

  @SuppressWarnings("unchecked")
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
