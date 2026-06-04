package com.zblog.content.application;

import com.zblog.common.exception.BusinessException;
import com.zblog.common.util.Slugify;
import com.zblog.content.application.MarkdownRenderer.RenderedContent;
import com.zblog.content.application.port.ArticleAssetStore;
import com.zblog.content.application.port.ArticleCommandRepository;
import com.zblog.content.application.port.ArticleImportSupportRepository;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleImportService {

  private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[[^]]*]\\(([^)]+)\\)");

  private final ArticleCommandRepository articleRepository;
  private final ArticleImportSupportRepository importSupportRepository;
  private final ArticleAssetStore articleAssetStore;
  private final MarkdownRenderer markdownRenderer;

  public ArticleImportService(
      ArticleCommandRepository articleRepository,
      ArticleImportSupportRepository importSupportRepository,
      ArticleAssetStore articleAssetStore,
      MarkdownRenderer markdownRenderer) {
    this.articleRepository = articleRepository;
    this.importSupportRepository = importSupportRepository;
    this.articleAssetStore = articleAssetStore;
    this.markdownRenderer = markdownRenderer;
  }

  @Transactional
  public Map<String, Object> importArticles(String sourceType, List<ImportedArticleFile> files) {
    List<Map<String, Object>> errors = new java.util.ArrayList<>();
    int success = 0;
    int categoriesAdded = 0;
    int tagsAdded = 0;
    for (ImportedArticleFile file : files == null ? List.<ImportedArticleFile>of() : files) {
      String filename = file.filename() == null ? "article.md" : file.filename();
      try {
        String markdown = new String(file.content(), StandardCharsets.UTF_8);
        validateMarkdownAssets(markdown);
        ParsedArticle parsed = parseArticle(filename, markdown, sourceType);
        TaxonomyEnsureResult category = ensureCategory(parsed.category());
        List<TaxonomyEnsureResult> tagResults = parsed.tags().stream().map(this::ensureTag).toList();
        if (category.created()) {
          categoriesAdded++;
        }
        tagsAdded += tagResults.stream().filter(TaxonomyEnsureResult::created).count();
        createImportedArticle(
            parsed.title(),
            uniqueSlug(parsed.slug()),
            parsed.markdown(),
            parsed.summary(),
            category.id(),
            tagResults.stream().map(TaxonomyEnsureResult::id).toList());
        success++;
      } catch (Exception exception) {
        errors.add(Map.of("filename", filename, "title", "", "error", exception.getMessage()));
      }
    }
    return Map.of(
        "total",
        files == null ? 0 : files.size(),
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

  private void createImportedArticle(
      String title, String slug, String markdown, String summary, long categoryId, List<Long> tagIds) {
    RenderedContent rendered = markdownRenderer.render(markdown);
    try {
      articleRepository.create(
          title,
          slug,
          markdown,
          rendered.html(),
          rendered.text(),
          summary,
          null,
          categoryId,
          tagIds,
          null,
          false,
          false,
          false);
    } catch (DuplicateKeyException exception) {
      throw new BusinessException(40901, "Article slug already exists: " + slug, HttpStatus.CONFLICT);
    }
  }

  private void validateMarkdownAssets(String markdown) {
    Matcher matcher = MARKDOWN_IMAGE_PATTERN.matcher(markdown == null ? "" : markdown);
    while (matcher.find()) {
      String url = matcher.group(1).trim();
      if (!isSupportedMarkdownImage(url)) {
        throw new IllegalArgumentException("unsupported markdown image: " + url);
      }
    }
  }

  private boolean isSupportedMarkdownImage(String url) {
    return url.startsWith("/uploads/") && articleAssetStore.existsUpload(url);
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

  private TaxonomyEnsureResult ensureCategory(String name) {
    String categoryName = name == null || name.isBlank() ? "默认分类" : stripYamlValue(name);
    return importSupportRepository.ensureCategory(categoryName, Slugify.from(categoryName));
  }

  private TaxonomyEnsureResult ensureTag(String name) {
    String tagName = stripYamlValue(name);
    return importSupportRepository.ensureTag(tagName, Slugify.from(tagName));
  }

  private String uniqueSlug(String base) {
    String slug = base;
    int index = 2;
    while (importSupportRepository.articleSlugExists(slug)) {
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
    if (value == null || value.isBlank()) {
      return List.of();
    }
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

  private record ParsedArticle(
      String title, String slug, String markdown, String summary, String category, List<String> tags) {}
}
