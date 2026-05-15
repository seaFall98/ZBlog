package com.zblog.content.application;

import com.zblog.common.api.PageResponse;
import com.zblog.common.util.Slugify;
import com.zblog.content.application.MarkdownRenderer.RenderedContent;
import com.zblog.content.infrastructure.JdbcArticleRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ArticleService {

  private final JdbcArticleRepository articleRepository;
  private final MarkdownRenderer markdownRenderer;
  private final JdbcTemplate jdbcTemplate;

  public ArticleService(
      JdbcArticleRepository articleRepository,
      MarkdownRenderer markdownRenderer,
      JdbcTemplate jdbcTemplate) {
    this.articleRepository = articleRepository;
    this.markdownRenderer = markdownRenderer;
    this.jdbcTemplate = jdbcTemplate;
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
    String markdown = textOrDefault(request, "content", value(existing, "content_markdown"));
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

  @Transactional
  public Map<String, Object> importArticles(String sourceType, MultipartFile[] files) {
    List<Map<String, Object>> errors = new ArrayList<>();
    int success = 0;
    int categoriesAdded = 0;
    int tagsAdded = 0;
    for (MultipartFile file : files == null ? new MultipartFile[0] : files) {
      String filename = file.getOriginalFilename() == null ? "article.md" : file.getOriginalFilename();
      try {
        String markdown = new String(file.getBytes(), StandardCharsets.UTF_8);
        ParsedArticle parsed = parseArticle(filename, markdown, sourceType);
        InsertResult category = ensureCategory(parsed.category());
        List<InsertResult> tagResults = parsed.tags().stream().map(this::ensureTag).toList();
        long categoryId = category.id();
        List<Long> tags = tagResults.stream().map(InsertResult::id).toList();
        if (category.created()) {
          categoriesAdded++;
        }
        tagsAdded += tagResults.stream().filter(InsertResult::created).count();
        create(
            Map.of(
                "title",
                parsed.title(),
                "slug",
                uniqueSlug(parsed.slug()),
                "content",
                parsed.markdown(),
                "summary",
                parsed.summary(),
                "category_id",
                categoryId,
                "tag_ids",
                tags));
        success++;
      } catch (Exception exception) {
        errors.add(Map.of("filename", filename, "title", "", "error", exception.getMessage()));
      }
    }
    return Map.of(
        "total",
        files == null ? 0 : files.length,
        "success",
        success,
        "failed",
        errors.size(),
        "categories_added",
        categoriesAdded,
        "tags_added",
        tagsAdded,
        "errors",
        errors);
  }

  public Map<String, Object> exportToWeChat(long id) {
    Map<String, Object> article = articleRepository.getAdmin(id);
    return Map.of("html", article.get("content") == null ? "" : article.get("content").toString());
  }

  public byte[] downloadMarkdownZip(long id) {
    Map<String, Object> article = articleRepository.getAdmin(id);
    try {
      java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
      try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
        String filename = article.get("slug") + ".md";
        zip.putNextEntry(new ZipEntry(filename));
        zip.write(value(article, "content_markdown").getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
      }
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to export article", exception);
    }
  }

  private ParsedArticle parseArticle(String filename, String raw, String sourceType) {
    String markdown = raw == null ? "" : raw.replace("\r\n", "\n");
    Map<String, String> frontMatter = new LinkedHashMap<>();
    if ("hexo".equals(sourceType) && markdown.startsWith("---\n")) {
      int end = markdown.indexOf("\n---", 4);
      if (end > 0) {
        String meta = markdown.substring(4, end).trim();
        markdown = markdown.substring(end + 4).trim();
        for (String line : meta.split("\n")) {
          int colon = line.indexOf(':');
          if (colon > 0) {
            frontMatter.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
          }
        }
      }
    }
    String title = frontMatter.getOrDefault("title", firstHeading(markdown));
    if (title.isBlank()) {
      title = filename.replaceFirst("\\.(md|markdown)$", "");
    }
    String summary = frontMatter.getOrDefault("description", firstParagraph(markdown));
    String category = frontMatter.getOrDefault("category", frontMatter.getOrDefault("categories", "默认分类"));
    List<String> tags = parseTags(frontMatter.get("tags"));
    return new ParsedArticle(title, Slugify.from(title), markdown, summary, category, tags);
  }

  private InsertResult ensureCategory(String name) {
    String categoryName = name == null || name.isBlank() ? "默认分类" : stripYamlValue(name);
    String slug = Slugify.from(categoryName);
    List<Long> existing =
        jdbcTemplate.query("select id from categories where slug = ?", (rs, rowNum) -> rs.getLong("id"), slug);
    if (!existing.isEmpty()) return new InsertResult(existing.getFirst(), false);
    return new InsertResult(
        insertAndReturnId(
            "insert into categories (name, slug, description, sort_order) values (?, ?, '', 0)",
            categoryName,
            slug),
        true);
  }

  private InsertResult ensureTag(String name) {
    String tagName = stripYamlValue(name);
    String slug = Slugify.from(tagName);
    List<Long> existing =
        jdbcTemplate.query("select id from tags where slug = ?", (rs, rowNum) -> rs.getLong("id"), slug);
    if (!existing.isEmpty()) return new InsertResult(existing.getFirst(), false);
    return new InsertResult(
        insertAndReturnId("insert into tags (name, slug, description) values (?, ?, '')", tagName, slug),
        true);
  }

  private long insertAndReturnId(String sql, Object... args) {
    org.springframework.jdbc.support.KeyHolder keyHolder =
        new org.springframework.jdbc.support.GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
          for (int i = 0; i < args.length; i++) {
            statement.setObject(i + 1, args[i]);
          }
          return statement;
        },
        keyHolder);
    Map<String, Object> keys = keyHolder.getKeys();
    if (keys != null && keys.get("id") instanceof Number number) return number.longValue();
    return keyHolder.getKey().longValue();
  }

  private String uniqueSlug(String base) {
    String slug = base;
    int index = 2;
    while (Boolean.TRUE.equals(
        jdbcTemplate.queryForObject("select count(*) > 0 from articles where slug = ?", Boolean.class, slug))) {
      slug = base + "-" + index++;
    }
    return slug;
  }

  private String firstHeading(String markdown) {
    for (String line : markdown.split("\n")) {
      if (line.matches("^#{1,6}\\s+.+")) {
        return line.replaceFirst("^#{1,6}\\s+", "").trim();
      }
    }
    return "";
  }

  private String firstParagraph(String markdown) {
    for (String line : markdown.split("\n")) {
      String trimmed = line.trim();
      if (!trimmed.isBlank() && !trimmed.startsWith("#")) {
        return trimmed.length() > 180 ? trimmed.substring(0, 180) : trimmed;
      }
    }
    return "";
  }

  private List<String> parseTags(String value) {
    if (value == null || value.isBlank()) return List.of();
    String cleaned = value.replace("[", "").replace("]", "");
    return Pattern.compile(",")
        .splitAsStream(cleaned)
        .map(this::stripYamlValue)
        .filter(item -> !item.isBlank())
        .toList();
  }

  private String stripYamlValue(String value) {
    return value == null ? "" : value.trim().replaceAll("^['\\\"]|['\\\"]$", "");
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

  private record ParsedArticle(
      String title, String slug, String markdown, String summary, String category, List<String> tags) {}

  private record InsertResult(long id, boolean created) {}
}
