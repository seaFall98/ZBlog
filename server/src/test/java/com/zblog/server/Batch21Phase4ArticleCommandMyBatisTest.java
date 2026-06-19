package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.zblog.content.application.ArticleService;
import com.zblog.content.application.port.ArticleCommandRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Batch21Phase4ArticleCommandMyBatisTest {

  @Autowired private ArticleCommandRepository articleCommandRepository;

  @Autowired private ApplicationContext applicationContext;

  @Autowired private ArticleService articleService;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final List<Long> createdArticleIds = new ArrayList<>();

  @AfterEach
  void cleanCreatedArticleOutboxEvents() {
    for (Long articleId : createdArticleIds) {
      jdbcTemplate.update("delete from event_outbox where aggregate_type = 'article' and aggregate_id = ?", articleId);
    }
    createdArticleIds.clear();
  }

  @Test
  void articleCommandRepositoryUsesMyBatisAdapterAndKeepsApplicationTransactionBoundary()
      throws Exception {
    assertThat(AopUtils.getTargetClass(articleCommandRepository).getName())
        .isEqualTo("com.zblog.content.infrastructure.mybatis.MyBatisArticleCommandRepository");
    assertThat(applicationContext.getBeansOfType(articleCommandRepositoryClass()).values())
        .noneSatisfy(
            bean ->
                assertThat(AopUtils.getTargetClass(bean).getName())
                    .isEqualTo("com.zblog.content.infrastructure.JdbcArticleCommandRepository"));
    assertTransactional("create", Map.class);
    assertTransactional("update", long.class, Map.class);
    assertTransactional("publish", long.class);
    assertTransactional("unpublish", long.class);
    assertTransactional("delete", long.class);
  }

  @Test
  void createKeepsDraftFieldsTagsAndResponseShape() {
    HttpHeaders headers = authenticatedHeaders();
    long categoryId = createCategory(headers, "Batch21 Phase4 Create Category", "batch21-phase4-create-category");
    long tagA = createTag(headers, "Batch21 Phase4 Create Tag A", "batch21-phase4-create-tag-a");
    long tagB = createTag(headers, "Batch21 Phase4 Create Tag B", "batch21-phase4-create-tag-b");

    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/articles",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.ofEntries(
                    Map.entry("title", "Batch21 Phase4 Create"),
                    Map.entry("slug", "batch21-phase4-create"),
                    Map.entry("summary", "Batch21 Phase4 create summary"),
                    Map.entry("content", "# Batch21 Phase4 Create\n\ncreate body"),
                    Map.entry("cover", "https://example.com/batch21-phase4-create.png"),
                    Map.entry("category_id", categoryId),
                    Map.entry("tag_ids", List.of(tagA, tagB)),
                    Map.entry("location", "phase4-create-location"),
                    Map.entry("is_top", true),
                    Map.entry("is_essence", true),
                    Map.entry("is_outdated", true)),
                headers),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> article = data(response);
    long articleId = number(article, "id");
    createdArticleIds.add(articleId);
    assertThat(article.get("slug")).isEqualTo("batch21-phase4-create");
    assertThat(article.get("title")).isEqualTo("Batch21 Phase4 Create");
    assertThat(article.get("summary")).isEqualTo("Batch21 Phase4 create summary");
    assertThat(article.get("content_markdown")).isEqualTo("# Batch21 Phase4 Create\n\ncreate body");
    assertThat(article.get("content").toString()).contains("<h1>Batch21 Phase4 Create</h1>");
    assertThat(article.get("is_publish")).isEqualTo(false);
    assertThat(article.get("is_top")).isEqualTo(true);
    assertThat(article.get("is_essence")).isEqualTo(true);
    assertThat(article.get("is_outdated")).isEqualTo(true);
    assertThat(article.get("location")).isEqualTo("phase4-create-location");
    assertThat(article.get("publish_time")).isNull();
    assertThat(article.get("update_time").toString()).contains("T").endsWith("Z");
    assertThat(((Map<?, ?>) article.get("category")).get("url"))
        .isEqualTo("/category/batch21-phase4-create-category");
    assertThat((List<?>) article.get("tags")).hasSize(2);
    assertThat(articleStatus(articleId)).isEqualTo("DRAFT");
    assertThat(tagIds(articleId)).containsExactly(tagA, tagB);
  }

  @Test
  void createPublishedArticleKeepsOutboxAndPublicSearchSideEffects() {
    HttpHeaders headers = authenticatedHeaders();

    long articleId =
        createArticle(
            headers,
            "batch21-phase4-create-published",
            "Batch21 Phase4 Create Published",
            "batch21-phase4-create-published-token",
            true);

    assertThat(articleStatus(articleId)).isEqualTo("PUBLISHED");
    assertThat(publishedAt(articleId)).isNotNull();
    assertThat(outboxCount("ARTICLE_PUBLISHED", articleId)).isEqualTo(1L);
    assertThat(outboxCount("ARTICLE_SEARCH_UPSERT", articleId)).isEqualTo(1L);
    assertPublicArticleExists("batch21-phase4-create-published");
    assertSearchContains("batch21-phase4-create-published-token", "batch21-phase4-create-published");
  }

  @Test
  void duplicateSlugStillReturns40901() {
    HttpHeaders headers = authenticatedHeaders();
    createArticle(headers, "batch21-phase4-duplicate", "Batch21 Phase4 Duplicate", "duplicate-token", false);

    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/articles",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "title", "Batch21 Phase4 Duplicate Again",
                    "slug", "batch21-phase4-duplicate",
                    "summary", "duplicate summary",
                    "content", "# Duplicate"),
                headers),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("code")).isEqualTo(40901);
    assertThat(articleCountBySlug("batch21-phase4-duplicate")).isEqualTo(1L);
  }

  @Test
	  void updateKeepsFieldOverwriteTagReplacementAndPublishedSearchUpsert() {
    HttpHeaders headers = authenticatedHeaders();
    long categoryId = createCategory(headers, "Batch21 Phase4 Update Category", "batch21-phase4-update-category");
    long tagA = createTag(headers, "Batch21 Phase4 Update Tag A", "batch21-phase4-update-tag-a");
    long tagB = createTag(headers, "Batch21 Phase4 Update Tag B", "batch21-phase4-update-tag-b");
    long articleId =
        createArticle(
            headers,
            "batch21-phase4-update",
            "Batch21 Phase4 Update",
            "batch21-phase4-update-initial-token",
            true,
            tagA);
    long beforeUpserts = outboxCount("ARTICLE_SEARCH_UPSERT", articleId);

    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/articles/" + articleId,
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.ofEntries(
                    Map.entry("title", "Batch21 Phase4 Updated"),
                    Map.entry("slug", "batch21-phase4-update"),
                    Map.entry("summary", "batch21-phase4-updated-token summary"),
                    Map.entry("content", "# Batch21 Phase4 Updated\n\nupdated body"),
                    Map.entry("cover", "https://example.com/batch21-phase4-updated.png"),
                    Map.entry("category_id", categoryId),
                    Map.entry("tag_ids", List.of(tagB)),
                    Map.entry("location", "phase4-updated-location"),
                    Map.entry("is_top", true),
                    Map.entry("is_essence", true),
                    Map.entry("is_outdated", true)),
                headers),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> row = articleRow(articleId);
    assertThat(row.get("title")).isEqualTo("Batch21 Phase4 Updated");
    assertThat(row.get("summary")).isEqualTo("batch21-phase4-updated-token summary");
    assertThat(row.get("content_markdown")).isEqualTo("# Batch21 Phase4 Updated\n\nupdated body");
    assertThat(row.get("category_id")).isEqualTo(categoryId);
    assertThat(row.get("location")).isEqualTo("phase4-updated-location");
    assertThat(row.get("is_top")).isEqualTo(true);
    assertThat(row.get("is_essence")).isEqualTo(true);
    assertThat(row.get("is_outdated")).isEqualTo(true);
    assertThat(updatedAt(articleId)).isNotNull();
    assertThat(tagIds(articleId)).containsExactly(tagB);
    assertThat(outboxCount("ARTICLE_SEARCH_UPSERT", articleId)).isEqualTo(beforeUpserts + 1);
	  }

  @Test
  void articleCopyrightMetadataPersistsForAdminAndPublicArticleViews() {
    HttpHeaders headers = authenticatedHeaders();
    ResponseEntity<Map> created =
        restTemplate.exchange(
            "/api/v1/admin/articles",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.ofEntries(
                    Map.entry("title", "Batch21 P4 Copyright"),
                    Map.entry("slug", "batch21-p4-copyright"),
                    Map.entry("summary", "copyright summary"),
                    Map.entry("content", "# Batch21 P4 Copyright\n\nbody"),
                    Map.entry("copyright_type", "REPOST"),
                    Map.entry("source_url", "https://example.com/original"),
                    Map.entry("source_title", "Original article"),
                    Map.entry("copyright_license", "CC BY-NC-SA 4.0"),
                    Map.entry("is_publish", true)),
                headers),
            Map.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> createdArticle = data(created);
    long articleId = number(createdArticle, "id");
    createdArticleIds.add(articleId);
    assertThat(createdArticle.get("copyright_type")).isEqualTo("REPOST");
    assertThat(createdArticle.get("source_url")).isEqualTo("https://example.com/original");
    assertThat(createdArticle.get("source_title")).isEqualTo("Original article");
    assertThat(createdArticle.get("copyright_license")).isEqualTo("CC BY-NC-SA 4.0");

    Map<?, ?> publicArticle = data(restTemplate.getForEntity("/api/v1/articles/batch21-p4-copyright", Map.class));
    assertThat(publicArticle.get("copyright_type")).isEqualTo("REPOST");
    assertThat(publicArticle.get("source_url")).isEqualTo("https://example.com/original");
    assertThat(publicArticle.get("source_title")).isEqualTo("Original article");
    assertThat(publicArticle.get("copyright_license")).isEqualTo("CC BY-NC-SA 4.0");

    ResponseEntity<Map> updated =
        restTemplate.exchange(
            "/api/v1/admin/articles/" + articleId,
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.ofEntries(
                    Map.entry("title", "Batch21 P4 Copyright Updated"),
                    Map.entry("slug", "batch21-p4-copyright"),
                    Map.entry("summary", "copyright updated summary"),
                    Map.entry("content", "# Batch21 P4 Copyright Updated\n\nbody"),
                    Map.entry("copyright_type", "TRANSLATION"),
                    Map.entry("source_url", "https://example.com/translated-source"),
                    Map.entry("source_title", "Translated source"),
                    Map.entry("copyright_license", "CC BY 4.0"),
                    Map.entry("is_publish", true)),
                headers),
            Map.class);

    assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> updatedArticle = data(updated);
    assertThat(updatedArticle.get("copyright_type")).isEqualTo("TRANSLATION");
    assertThat(updatedArticle.get("source_url")).isEqualTo("https://example.com/translated-source");
    assertThat(updatedArticle.get("source_title")).isEqualTo("Translated source");
    assertThat(updatedArticle.get("copyright_license")).isEqualTo("CC BY 4.0");
  }

  @Test
  void publishKeepsPublishedAtAndRepeatPublishDoesNotRepeatEvents() {
    HttpHeaders headers = authenticatedHeaders();
    long articleId =
        createArticle(
            headers,
            "batch21-phase4-repeat-publish",
            "Batch21 Phase4 Repeat Publish",
            "batch21-phase4-repeat-publish-token",
            false);

    ResponseEntity<Map> first = publish(headers, articleId);
    assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
    Object firstPublishedAt = publishedAt(articleId);
    long publishedEvents = outboxCount("ARTICLE_PUBLISHED", articleId);
    long upsertEvents = outboxCount("ARTICLE_SEARCH_UPSERT", articleId);

    ResponseEntity<Map> second = publish(headers, articleId);

    assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(publishedAt(articleId)).isEqualTo(firstPublishedAt);
    assertThat(outboxCount("ARTICLE_PUBLISHED", articleId)).isEqualTo(publishedEvents);
    assertThat(outboxCount("ARTICLE_SEARCH_UPSERT", articleId)).isEqualTo(upsertEvents);
  }

  @Test
  void unpublishKeepsPublishedAtAndCreatesSearchDelete() {
    HttpHeaders headers = authenticatedHeaders();
    long articleId =
        createArticle(
            headers,
            "batch21-phase4-unpublish",
            "Batch21 Phase4 Unpublish",
            "batch21-phase4-unpublish-token",
            true);
    Object originalPublishedAt = publishedAt(articleId);
    long beforeDeletes = outboxCount("ARTICLE_SEARCH_DELETE", articleId);

    ResponseEntity<Map> response = unpublish(headers, articleId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(articleStatus(articleId)).isEqualTo("DRAFT");
    assertThat(publishedAt(articleId)).isEqualTo(originalPublishedAt);
    assertThat(outboxCount("ARTICLE_SEARCH_DELETE", articleId)).isEqualTo(beforeDeletes + 1);
    assertPublicArticleNotFound("batch21-phase4-unpublish");
    assertSearchDoesNotContain("batch21-phase4-unpublish-token", "batch21-phase4-unpublish");
  }

  @Test
  void deletePublishedArticleCreatesSearchDeleteAndRemovesArticle() {
    HttpHeaders headers = authenticatedHeaders();
    long articleId =
        createArticle(
            headers,
            "batch21-phase4-delete",
            "Batch21 Phase4 Delete",
            "batch21-phase4-delete-token",
            true);
    long beforeDeletes = outboxCount("ARTICLE_SEARCH_DELETE", articleId);

    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/articles/" + articleId, HttpMethod.DELETE, new HttpEntity<>(headers), Map.class);
    createdArticleIds.remove(articleId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(articleExists(articleId)).isFalse();
    assertThat(outboxCount("ARTICLE_SEARCH_DELETE", articleId)).isEqualTo(beforeDeletes + 1);
    assertAdminArticleNotFound(headers, articleId);
    jdbcTemplate.update("delete from event_outbox where aggregate_type = 'article' and aggregate_id = ?", articleId);
  }

  @Test
  void tagFailuresStillRollbackCreateAndUpdate() {
    HttpHeaders headers = authenticatedHeaders();
    String createSlug = "batch21-phase4-create-tag-rollback";

    ResponseEntity<Map> createResponse =
        restTemplate.exchange(
            "/api/v1/admin/articles",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "title", "Batch21 Phase4 Create Tag Rollback",
                    "slug", createSlug,
                    "summary", "create rollback summary",
                    "content", "# Create rollback",
                    "tag_ids", List.of(987654321L)),
                headers),
            Map.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(articleCountBySlug(createSlug)).isZero();

    long stableTag = createTag(headers, "Batch21 Phase4 Stable Tag", "batch21-phase4-stable-tag");
    long articleId =
        createArticle(
            headers,
            "batch21-phase4-update-tag-rollback",
            "Batch21 Phase4 Stable",
            "batch21-phase4-stable-token",
            false,
            stableTag);
    Map<String, Object> before = articleRow(articleId);

    ResponseEntity<Map> updateResponse =
        restTemplate.exchange(
            "/api/v1/admin/articles/" + articleId,
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of(
                    "title", "Batch21 Phase4 Mutated",
                    "summary", "mutated summary",
                    "content", "# Mutated",
                    "tag_ids", List.of(987654322L)),
                headers),
            Map.class);

    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    Map<String, Object> after = articleRow(articleId);
    assertThat(after.get("title")).isEqualTo(before.get("title"));
    assertThat(after.get("summary")).isEqualTo(before.get("summary"));
    assertThat(after.get("content_markdown")).isEqualTo(before.get("content_markdown"));
    assertThat(tagIds(articleId)).containsExactly(stableTag);
  }

  @Test
  void articleImportServiceStillCreatesDraftArticleThroughCommandRepository() {
    HttpHeaders headers = authenticatedHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("source_type", "hexo");
    body.add(
        "files",
        resource(
            "batch21-phase4-import.md",
            """
            ---
            title: Batch21 Phase4 Imported
            description: Batch21 Phase4 imported summary
            ---
            # Batch21 Phase4 Imported

            Imported body.
            """));

    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/articles/import", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> result = data(response);
    assertThat(result.get("success")).isEqualTo(1);
    Map<String, Object> imported = articleByTitle("Batch21 Phase4 Imported");
    createdArticleIds.add(((Number) imported.get("id")).longValue());
    assertThat(imported.get("status")).isEqualTo("DRAFT");
    assertThat(imported.get("content_markdown").toString()).contains("# Batch21 Phase4 Imported");
    assertThat(imported.get("content_text").toString()).contains("Imported body");
    assertThat(imported.get("summary")).isEqualTo("Batch21 Phase4 imported summary");
  }

  private void assertTransactional(String methodName, Class<?>... parameterTypes) throws Exception {
    Class<?> serviceClass = AopUtils.getTargetClass(articleService);
    assertThat(
            serviceClass.getDeclaredMethod(methodName, parameterTypes).isAnnotationPresent(Transactional.class)
                || serviceClass.isAnnotationPresent(Transactional.class))
        .as("ArticleService#" + methodName + " should keep application transaction boundary")
        .isTrue();
  }

  private Class<?> articleCommandRepositoryClass() throws ClassNotFoundException {
    return Class.forName("com.zblog.content.application.port.ArticleCommandRepository");
  }

  private long createCategory(HttpHeaders headers, String name, String slug) {
    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/categories",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("name", name, "slug", slug, "description", "Batch21 Phase4", "sort", 10), headers),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return number(data(response), "id");
  }

  private long createTag(HttpHeaders headers, String name, String slug) {
    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/tags",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("name", name, "slug", slug, "description", "Batch21 Phase4"), headers),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return number(data(response), "id");
  }

  private long createArticle(
      HttpHeaders headers, String slug, String title, String keyword, boolean publish, Long... tagIds) {
    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/articles",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "title", title,
                    "slug", slug,
                    "summary", "summary " + keyword,
                    "content", "# " + title + "\n\nbody " + keyword,
                    "tag_ids", List.of(tagIds),
                    "is_publish", publish),
                headers),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    long articleId = number(data(response), "id");
    createdArticleIds.add(articleId);
    return articleId;
  }

  private ResponseEntity<Map> publish(HttpHeaders headers, long articleId) {
    return restTemplate.exchange(
        "/api/v1/admin/articles/" + articleId + "/publish",
        HttpMethod.POST,
        new HttpEntity<>(headers),
        Map.class);
  }

  private ResponseEntity<Map> unpublish(HttpHeaders headers, long articleId) {
    return restTemplate.exchange(
        "/api/v1/admin/articles/" + articleId + "/unpublish",
        HttpMethod.POST,
        new HttpEntity<>(headers),
        Map.class);
  }

  private void assertPublicArticleExists(String slug) {
    ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/articles/" + slug, Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  private void assertPublicArticleNotFound(String slug) {
    ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/articles/" + slug, Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  private void assertAdminArticleNotFound(HttpHeaders headers, long articleId) {
    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/articles/" + articleId, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  private void assertSearchContains(String keyword, String slug) {
    assertThat(search(keyword))
        .anySatisfy(row -> assertThat(((Map<?, ?>) row).get("slug")).isEqualTo(slug));
  }

  private void assertSearchDoesNotContain(String keyword, String slug) {
    assertThat(search(keyword))
        .noneSatisfy(row -> assertThat(((Map<?, ?>) row).get("slug")).isEqualTo(slug));
  }

  private List<?> search(String keyword) {
    ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/articles/search?keyword=" + keyword, Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return (List<?>) data(response).get("list");
  }

  private HttpHeaders authenticatedHeaders() {
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            "/api/v1/auth/login", Map.of("username", "admin", "password", "admin123456"), Map.class);
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(data(response).get("access_token").toString());
    return headers;
  }

  private ByteArrayResource resource(String filename, String content) {
    return new ByteArrayResource(content.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
      @Override
      public String getFilename() {
        return filename;
      }
    };
  }

  private String articleStatus(long articleId) {
    return jdbcTemplate.queryForObject("select status from articles where id = ?", String.class, articleId);
  }

  private Object publishedAt(long articleId) {
    return jdbcTemplate.queryForObject("select published_at from articles where id = ?", Object.class, articleId);
  }

  private Object updatedAt(long articleId) {
    return jdbcTemplate.queryForObject("select updated_at from articles where id = ?", Object.class, articleId);
  }

  private boolean articleExists(long articleId) {
    return jdbcTemplate.queryForObject("select count(*) from articles where id = ?", Long.class, articleId) == 1L;
  }

  private long articleCountBySlug(String slug) {
    return jdbcTemplate.queryForObject("select count(*) from articles where slug = ?", Long.class, slug);
  }

  private long outboxCount(String eventType, long articleId) {
    return jdbcTemplate.queryForObject(
        "select count(*) from event_outbox where event_type = ? and aggregate_id = ?",
        Long.class,
        eventType,
        articleId);
  }

  private Map<String, Object> articleRow(long articleId) {
    return jdbcTemplate.queryForMap(
        "select title, summary, content_markdown, category_id, location, is_top, is_essence, is_outdated from articles where id = ?",
        articleId);
  }

  private Map<String, Object> articleByTitle(String title) {
    return jdbcTemplate.queryForMap(
        "select id, status, content_markdown, content_text, summary from articles where title = ?",
        title);
  }

  private List<Long> tagIds(long articleId) {
    return jdbcTemplate.queryForList(
        "select tag_id from article_tags where article_id = ? order by tag_id", Long.class, articleId);
  }

  private Map<?, ?> data(ResponseEntity<Map> response) {
    assertThat(response.getBody()).isNotNull();
    return data(response.getBody());
  }

  private Map<?, ?> data(Map<?, ?> body) {
    assertThat(body.get("code")).isEqualTo(0);
    Object data = body.get("data");
    assertThat(data).isInstanceOf(Map.class);
    return (Map<?, ?>) data;
  }

  private long number(Map<?, ?> data, String key) {
    Object value = data.get(key);
    assertThat(value).isInstanceOf(Number.class);
    return ((Number) value).longValue();
  }
}
