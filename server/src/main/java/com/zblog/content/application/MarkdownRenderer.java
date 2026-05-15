package com.zblog.content.application;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class MarkdownRenderer {

  private static final Pattern IMAGE_PATTERN = Pattern.compile("^!\\[([^]]*)]\\(([^)]+)\\)$");

  public RenderedContent render(String markdown) {
    String source = markdown == null ? "" : markdown.trim();
    StringBuilder html = new StringBuilder();
    StringBuilder text = new StringBuilder();

    for (String line : source.split("\\R")) {
      String trimmed = line.trim();
      if (trimmed.isBlank()) {
        continue;
      }
      Matcher image = IMAGE_PATTERN.matcher(trimmed);
      if (trimmed.startsWith("# ")) {
        append(html, "h1", trimmed.substring(2));
      } else if (trimmed.startsWith("## ")) {
        append(html, "h2", trimmed.substring(3));
      } else if (image.matches()) {
        appendImage(html, image.group(1), image.group(2));
      } else {
        append(html, "p", trimmed);
      }
      text.append(trimmed.replaceFirst("^#+\\s*", "")).append(' ');
    }

    return new RenderedContent(html.toString(), text.toString().trim());
  }

  private void append(StringBuilder html, String tag, String value) {
    html.append('<')
        .append(tag)
        .append('>')
        .append(HtmlUtils.htmlEscape(value))
        .append("</")
        .append(tag)
        .append('>');
  }

  private void appendImage(StringBuilder html, String alt, String src) {
    html.append("<p><img src=\"")
        .append(HtmlUtils.htmlEscape(src))
        .append("\" alt=\"")
        .append(HtmlUtils.htmlEscape(alt))
        .append("\"></p>");
  }

  public record RenderedContent(String html, String text) {}
}
