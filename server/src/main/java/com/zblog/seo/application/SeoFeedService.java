package com.zblog.seo.application;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SeoFeedService {

  private final JdbcTemplate jdbcTemplate;

  public SeoFeedService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public String rss() {
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    xml.append("<rss version=\"2.0\"><channel>");
    xml.append("<title>ZBlog</title><link>/</link><description>ZBlog feed</description>");
    for (Map<String, Object> article : articles()) {
      xml.append("<item>");
      xml.append("<title>").append(escape(text(article, "title"))).append("</title>");
      xml.append("<link>").append(path(article)).append("</link>");
      xml.append("<guid>").append(path(article)).append("</guid>");
      xml.append("<description>").append(escape(text(article, "summary"))).append("</description>");
      xml.append("<pubDate>").append(instant(article, "published_at")).append("</pubDate>");
      xml.append("</item>");
    }
    xml.append("</channel></rss>");
    return xml.toString();
  }

  public String atom() {
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    xml.append("<feed xmlns=\"http://www.w3.org/2005/Atom\">");
    xml.append("<title>ZBlog</title><id>/</id><updated>").append(Instant.now()).append("</updated>");
    for (Map<String, Object> article : articles()) {
      xml.append("<entry>");
      xml.append("<title>").append(escape(text(article, "title"))).append("</title>");
      xml.append("<id>").append(path(article)).append("</id>");
      xml.append("<link href=\"").append(path(article)).append("\" />");
      xml.append("<updated>").append(instant(article, "updated_at")).append("</updated>");
      xml.append("<summary>").append(escape(text(article, "summary"))).append("</summary>");
      xml.append("</entry>");
    }
    xml.append("</feed>");
    return xml.toString();
  }

  public String sitemap() {
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
    xml.append(url("/"));
    xml.append(url("/archive"));
    xml.append(url("/categories"));
    xml.append(url("/tags"));
    for (Map<String, Object> article : articles()) {
      xml.append("<url><loc>").append(path(article)).append("</loc>");
      xml.append("<lastmod>").append(instant(article, "updated_at")).append("</lastmod>");
      xml.append("</url>");
    }
    xml.append("</urlset>");
    return xml.toString();
  }

  private List<Map<String, Object>> articles() {
    return jdbcTemplate.queryForList(
        """
        select slug, title, summary, published_at, updated_at
        from articles
        where status = 'PUBLISHED'
        order by published_at desc, id desc
        limit 100
        """);
  }

  private String url(String path) {
    return "<url><loc>" + path + "</loc><lastmod>" + Instant.now() + "</lastmod></url>";
  }

  private String path(Map<String, Object> article) {
    return "/posts/" + text(article, "slug");
  }

  private String text(Map<String, Object> row, String key) {
    Object value = row.get(key);
    return value == null ? "" : value.toString();
  }

  private String instant(Map<String, Object> row, String key) {
    Object value = row.get(key);
    if (value instanceof Timestamp timestamp) {
      return timestamp.toInstant().toString();
    }
    return Instant.now().toString();
  }

  private String escape(String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }
}
