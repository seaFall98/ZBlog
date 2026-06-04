package com.zblog.content.application;

import com.zblog.content.application.port.ArticleAssetStore;
import com.zblog.content.application.port.ArticleAdminQueryRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;

@Service
public class ArticleExportService {

  private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[[^]]*]\\(([^)]+)\\)");

  private final ArticleAdminQueryRepository articleRepository;
  private final ArticleAssetStore articleAssetStore;

  public ArticleExportService(ArticleAdminQueryRepository articleRepository, ArticleAssetStore articleAssetStore) {
    this.articleRepository = articleRepository;
    this.articleAssetStore = articleAssetStore;
  }

  public Map<String, Object> exportToWeChat(long id) {
    Map<String, Object> article = articleRepository.getAdmin(id);
    return Map.of("html", article.get("content") == null ? "" : article.get("content").toString());
  }

  public byte[] downloadMarkdownZip(long id) {
    Map<String, Object> article = articleRepository.getAdmin(id);
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
        String markdown = markdownForZip(value(article, "content_markdown"), zip);
        String filename = article.get("slug") + ".md";
        zip.putNextEntry(new ZipEntry(filename));
        zip.write(markdown.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
      }
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to export article", exception);
    }
  }

  private String markdownForZip(String markdown, ZipOutputStream zip) throws IOException {
    Matcher matcher = MARKDOWN_IMAGE_PATTERN.matcher(markdown == null ? "" : markdown);
    StringBuilder rewritten = new StringBuilder();
    Map<String, String> replacements = new LinkedHashMap<>();
    while (matcher.find()) {
      String url = matcher.group(1).trim();
      if (url.startsWith("/uploads/")) {
        String filename = url.substring("/uploads/".length());
        articleAssetStore.readUpload(url)
            .ifPresent(
                content -> {
                  try {
                    String entryName = "assets/" + filename;
                    zip.putNextEntry(new ZipEntry(entryName));
                    zip.write(content);
                    zip.closeEntry();
                    replacements.put(url, entryName);
                  } catch (IOException exception) {
                    throw new IllegalStateException("Failed to export article", exception);
                  }
                });
      }
    }
    matcher.reset();
    while (matcher.find()) {
      String url = matcher.group(1).trim();
      String replacement = replacements.get(url);
      if (replacement != null) {
        matcher.appendReplacement(rewritten, Matcher.quoteReplacement(matcher.group().replace(url, replacement)));
      }
    }
    matcher.appendTail(rewritten);
    return rewritten.toString();
  }

  private String value(Map<String, Object> map, String key) {
    Object value = map.get(key);
    return value == null ? "" : value.toString();
  }
}
