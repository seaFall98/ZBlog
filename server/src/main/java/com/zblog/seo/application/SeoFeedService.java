package com.zblog.seo.application;

import com.zblog.seo.application.port.SeoFeedRepository;
import com.zblog.seo.application.port.SeoFeedRepository.FeedArticle;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SeoFeedService {

  private final SeoFeedRepository seoFeedRepository;
  private final String publicSiteUrl;

  public SeoFeedService(
      SeoFeedRepository seoFeedRepository,
      @Value("${zblog.seo.public-site-url:}") String publicSiteUrl) {
    this.seoFeedRepository = seoFeedRepository;
    this.publicSiteUrl = normalizeBaseUrl(publicSiteUrl);
  }

  public String rss() {
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    xml.append("<rss version=\"2.0\"><channel>");
    xml.append("<title>ZBlog</title><link>")
        .append(escape(absoluteUrl("/")))
        .append("</link><description>ZBlog feed</description>");
    for (FeedArticle article : articles()) {
      String articleUrl = absoluteUrl(path(article));
      xml.append("<item>");
      xml.append("<title>").append(escape(article.title())).append("</title>");
      xml.append("<link>").append(escape(articleUrl)).append("</link>");
      xml.append("<guid>").append(escape(articleUrl)).append("</guid>");
      xml.append("<description>").append(escape(article.summary())).append("</description>");
      xml.append("<pubDate>").append(instant(article.publishedAt())).append("</pubDate>");
      xml.append("</item>");
    }
    xml.append("</channel></rss>");
    return xml.toString();
  }

  public String atom() {
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    xml.append("<feed xmlns=\"http://www.w3.org/2005/Atom\">");
    xml.append("<title>ZBlog</title><id>")
        .append(escape(absoluteUrl("/")))
        .append("</id><updated>")
        .append(Instant.now())
        .append("</updated>");
    for (FeedArticle article : articles()) {
      String articleUrl = absoluteUrl(path(article));
      xml.append("<entry>");
      xml.append("<title>").append(escape(article.title())).append("</title>");
      xml.append("<id>").append(escape(articleUrl)).append("</id>");
      xml.append("<link href=\"").append(escape(articleUrl)).append("\" />");
      xml.append("<updated>").append(instant(article.updatedAt())).append("</updated>");
      xml.append("<summary>").append(escape(article.summary())).append("</summary>");
      xml.append("</entry>");
    }
    xml.append("</feed>");
    return xml.toString();
  }

  public String sitemap() {
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    xml.append(
        "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\" xmlns:image=\"http://www.google.com/schemas/sitemap-image/1.1\">");
    xml.append(url("/"));
    xml.append(url("/archive"));
    xml.append(url("/categories"));
    xml.append(url("/tags"));
    for (FeedArticle article : articles()) {
      xml.append("<url><loc>").append(escape(absoluteUrl(path(article)))).append("</loc>");
      xml.append("<lastmod>").append(instant(article.updatedAt())).append("</lastmod>");
      String cover = article.coverUrl();
      if (!cover.isBlank()) {
        xml.append("<image:image><image:loc>")
            .append(escape(absoluteUrl(cover)))
            .append("</image:loc><image:title>")
            .append(escape(article.title()))
            .append("</image:title></image:image>");
      }
      xml.append("</url>");
    }
    xml.append("</urlset>");
    return xml.toString();
  }

  public String robots() {
    return String.join(
        "\n",
        "User-agent: *",
        "Allow: /",
        "Disallow: /admin/",
        "Disallow: /profile",
        "Disallow: /notifications",
        "Disallow: /oauth/",
        "Disallow: /feedback/query",
        "",
        "Sitemap: " + absoluteUrl("/sitemap.xml"),
        "");
  }

  private java.util.List<FeedArticle> articles() {
    return seoFeedRepository.publishedFeedArticles();
  }

  private String url(String path) {
    return "<url><loc>"
        + escape(absoluteUrl(path))
        + "</loc><lastmod>"
        + Instant.now()
        + "</lastmod></url>";
  }

  private String path(FeedArticle article) {
    return "/posts/" + article.slug();
  }

  private String absoluteUrl(String pathOrUrl) {
    if (pathOrUrl == null || pathOrUrl.isBlank()) {
      return publicSiteUrl.isBlank() ? "/" : publicSiteUrl + "/";
    }
    String value = pathOrUrl.trim();
    if (value.startsWith("http://") || value.startsWith("https://")) {
      return value;
    }
    if (publicSiteUrl.isBlank()) {
      return value;
    }
    return publicSiteUrl + (value.startsWith("/") ? value : "/" + value);
  }

  private String normalizeBaseUrl(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return value.trim().replaceAll("/+$", "");
  }

  private String instant(Instant value) {
    return value == null ? Instant.now().toString() : value.toString();
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
