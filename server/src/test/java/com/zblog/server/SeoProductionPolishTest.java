package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "zblog.seo.public-site-url=https://blog.example.test")
class SeoProductionPolishTest {

  private static final String SLUG = "batch-20-seo-production-polish";
  private static final String DRAFT_SLUG = "batch-20-seo-production-draft";
  private static final String COVER = "/uploads/batch20-cover.png";

  @Autowired private TestRestTemplate restTemplate;

  @AfterEach
  void cleanup() {
    HttpHeaders headers = authenticatedHeaders();
    deleteBySlug(SLUG, headers);
    deleteBySlug(DRAFT_SLUG, headers);
  }

  @Test
  void sitemapUsesPublicCanonicalUrlsAndExcludesPrivateRoutesAndDrafts() {
    HttpHeaders headers = authenticatedHeaders();
    createArticle(SLUG, "Batch 20 SEO Published", true, COVER, headers);
    createArticle(DRAFT_SLUG, "Batch 20 SEO Draft", false, null, headers);

    String sitemap = xml("/sitemap.xml");

    assertThat(sitemap).contains("<urlset");
    assertThat(sitemap).contains("https://blog.example.test/");
    assertThat(sitemap).contains("https://blog.example.test/posts/" + SLUG);
    assertThat(sitemap).contains("https://blog.example.test" + COVER);
    assertThat(sitemap).contains("xmlns:image=\"http://www.google.com/schemas/sitemap-image/1.1\"");
    assertThat(sitemap).contains("<image:loc>https://blog.example.test" + COVER + "</image:loc>");
    assertThat(sitemap).doesNotContain("/posts/" + DRAFT_SLUG);
    assertThat(sitemap).doesNotContain("/profile", "/notifications", "/oauth/callback", "/feedback/query", "/admin");
    assertThat(sitemap).doesNotContain("localhost");
  }

  @Test
  void feedsUsePublicCanonicalUrlsAndExcludeDrafts() {
    HttpHeaders headers = authenticatedHeaders();
    createArticle(SLUG, "Batch 20 SEO Published", true, COVER, headers);
    createArticle(DRAFT_SLUG, "Batch 20 SEO Draft", false, null, headers);

    String rss = xml("/rss.xml");
    String atom = xml("/atom.xml");

    assertThat(rss).contains("<link>https://blog.example.test/</link>");
    assertThat(rss).contains("<title>寂静之书</title>");
    assertThat(rss).contains("<description>记录平凡生活里的光与影，写作是一种安静的对话。</description>");
    assertThat(rss).contains("<link>https://blog.example.test/posts/" + SLUG + "</link>");
    assertThat(rss).contains("<guid>https://blog.example.test/posts/" + SLUG + "</guid>");
    assertThat(rss).doesNotContain("/posts/" + DRAFT_SLUG, "localhost", "<title>ZBlog</title>", "ZBlog feed");

    assertThat(atom).contains("<id>https://blog.example.test/</id>");
    assertThat(atom).contains("<title>寂静之书</title>");
    assertThat(atom).contains("<subtitle>记录平凡生活里的光与影，写作是一种安静的对话。</subtitle>");
    assertThat(atom).contains("<id>https://blog.example.test/posts/" + SLUG + "</id>");
    assertThat(atom).contains("<link href=\"https://blog.example.test/posts/" + SLUG + "\" />");
    assertThat(atom).doesNotContain("/posts/" + DRAFT_SLUG, "localhost", "<title>ZBlog</title>", "ZBlog feed");
  }

  @Test
  void robotsReferencesPublicSitemapAndDisallowsPrivateRoutes() {
    ResponseEntity<String> response = restTemplate.getForEntity("/robots.txt", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody())
        .contains("User-agent: *")
        .contains("Allow: /")
        .contains("Disallow: /admin/")
        .contains("Disallow: /profile")
        .contains("Disallow: /notifications")
        .contains("Disallow: /oauth/")
        .contains("Disallow: /feedback/query")
        .contains("Sitemap: https://blog.example.test/sitemap.xml")
        .doesNotContain("Disallow: /uploads");
  }

  @Test
  void frontConfigUsesOwnerApprovedReferenceDefaultsForSeoRelevantIdentity() {
    ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/front/config", Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> config = (Map<?, ?>) data(response);
    Map<?, ?> identity = (Map<?, ?>) config.get("identity");
    Map<?, ?> home = (Map<?, ?>) config.get("home");
    Map<?, ?> about = (Map<?, ?>) config.get("about");
    Map<?, ?> footer = (Map<?, ?>) config.get("footer");
    Map<?, ?> guestbook = (Map<?, ?>) config.get("guestbook");
    List<?> skillItems = (List<?>) about.get("skillItems");
    List<?> timelineItems = (List<?>) about.get("timelineItems");
    List<?> socialLinks = (List<?>) footer.get("socialLinks");

    assertThat(identity.get("siteTitle")).isEqualTo("寂静之书");
    assertThat(identity.get("ownerDisplayName")).isEqualTo("Z");
    assertThat(identity.get("email")).isEqualTo("zz1362410372@gmail.com");
    assertThat(identity.get("primaryImageUrl"))
        .isEqualTo("/uploads/1782184837997_0f0990627d6a8491243fb20c34c097c2.png");
    assertThat(identity.get("faviconUrl"))
        .isEqualTo("/uploads/1782184837996_ChatGPT_Image_May_19__2026__02_05_47_AM.png");
    assertThat(home.get("heroEyebrow")).isEqualTo("Z的小站");
    assertThat(home.get("heroTitle")).isEqualTo("以文字作舟\n渡光阴\n之河");
    assertThat(about.get("introText").toString()).contains("一个喜欢在平凡生活里寻找微小美好的人");
    assertThat(about.get("bottomQuote")).isEqualTo("生活就是很多很多个平凡的日子，偶尔有一些光。");
    assertThat(skillItems).hasSize(5);
    assertThat(skillItems.toString()).contains("写作", "摄影", "阅读", "旅行", "设计");
    assertThat(timelineItems.toString()).contains("第一次独自旅行，去了日本京都", "建立这个博客，写下第一篇文章");
    assertThat(guestbook.get("introText")).isEqualTo("欢迎留言~");
    assertThat(guestbook.get("backgroundImage"))
        .isEqualTo("/uploads/1782184837997_ChatGPT_Image_2026_6_12__06_00_01__4_.png");
    assertThat(guestbook.get("danmakuPublicLimit")).isEqualTo(300);
    assertThat(footer.get("description")).isEqualTo("记录平凡生活里的光与影，写作是一种安静的对话。");
    assertThat(socialLinks.toString()).contains("mailto:zz1362410372@gmail.com");
    assertThat(config.toString()).doesNotContain("Paico", "Modern Blog", "Build in public");
  }

  @Test
  void ownerApprovedDefaultUploadAssetsAreBundledForFreshDeployments() {
    List.of(
            "1782184837997_0f0990627d6a8491243fb20c34c097c2.png",
            "1782184837996_ChatGPT_Image_May_19__2026__02_05_47_AM.png",
            "1782184837997_ChatGPT_Image_2026_6_12__06_00_01__4_.png")
        .forEach(
            filename ->
                assertThat(getClass().getClassLoader().getResource("db/default-uploads/" + filename)).isNotNull());
  }

  private Number createArticle(String slug, String title, boolean publish, String cover, HttpHeaders headers) {
    Map<String, Object> body = new java.util.LinkedHashMap<>();
    body.put("title", title);
    body.put("slug", slug);
    body.put("summary", title + " summary");
    body.put("content", "# " + title + "\n\nBatch20 SEO body");
    body.put("is_publish", publish);
    if (cover != null) {
      body.put("cover", cover);
    }
    Map<?, ?> article =
        (Map<?, ?>)
            data(
                restTemplate.exchange(
                    "/api/v1/admin/articles",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class));
    return (Number) article.get("id");
  }

  private String xml(String path) {
    ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotBlank();
    return response.getBody();
  }

  private void deleteBySlug(String slug, HttpHeaders headers) {
    ResponseEntity<Map> listResponse =
        restTemplate.exchange(
            "/api/v1/admin/articles?keyword=" + slug + "&page_size=20",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);
    if (!HttpStatus.OK.equals(listResponse.getStatusCode())) {
      return;
    }
    Map<?, ?> page = (Map<?, ?>) data(listResponse);
    for (Object row : (java.util.List<?>) page.get("list")) {
      Map<?, ?> article = (Map<?, ?>) row;
      if (slug.equals(article.get("slug"))) {
        restTemplate.exchange(
            "/api/v1/admin/articles/" + article.get("id"),
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            Map.class);
      }
    }
  }

  private HttpHeaders authenticatedHeaders() {
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            "/api/v1/auth/login", Map.of("username", "admin", "password", "admin123456"), Map.class);
    String token = ((Map<?, ?>) data(response)).get("access_token").toString();
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private Object data(ResponseEntity<Map> response) {
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("code")).isEqualTo(0);
    return body.get("data");
  }
}
