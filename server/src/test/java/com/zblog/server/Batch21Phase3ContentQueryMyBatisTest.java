package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.zblog.content.application.port.ArticleAdminQueryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Batch21Phase3ContentQueryMyBatisTest {

  @Autowired private ArticleAdminQueryRepository articleAdminQueryRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void articleAdminQueryRepositoryUsesMyBatisInfrastructureAdapter() {
    assertThat(articleAdminQueryRepository.getClass().getName())
        .contains("com.zblog.content.infrastructure.mybatis.MyBatisArticleAdminQueryRepository");
  }

  @Test
  void adminArticleListKeepsPagingFilterAndTagsContract() {
    HttpHeaders headers = authenticatedHeaders();
    long categoryId = insertCategory("Batch21 Phase3 Category", "batch21-phase3-category");
    long tagA = insertTag("Batch21 Phase3 Tag A", "batch21-phase3-tag-a");
    long tagB = insertTag("Batch21 Phase3 Tag B", "batch21-phase3-tag-b");
    insertArticle(
        "batch21-phase3-admin-match",
        "Batch21 Phase3 Admin Match",
        true,
        true,
        true,
        true,
        categoryId,
        List.of(tagA, tagB),
        "Batch21City",
        LocalDate.now().atTime(10, 0));
    insertArticle(
        "batch21-phase3-admin-other",
        "Batch21 Phase3 Admin Other",
        true,
        false,
        false,
        false,
        null,
        List.of(),
        "OtherCity",
        LocalDate.now().atTime(9, 0));
    insertArticleWithContent(
        "batch21-phase3-md-only",
        "Batch21 Phase3 Md Only",
        "No matching keyword summary",
        "# markdown-only-token",
        "<h1>markdown-only-token</h1>",
        "No matching keyword text",
        true,
        null,
        List.of(),
        "MarkdownOnlyCity",
        LocalDate.now().atTime(8, 0));

    assertArticleSlugs(adminGet("/api/v1/admin/articles?keyword=markdown-only-token", headers));

    Map<?, ?> byCategory = adminGet("/api/v1/admin/articles?category_id=" + categoryId, headers);
    assertPageShape(byCategory, 1, 10);
    assertArticleSlugs(byCategory, "batch21-phase3-admin-match");

    Map<?, ?> byTag = adminGet("/api/v1/admin/articles?tag_ids=" + tagA + "&tag_ids=" + tagB, headers);
    assertArticleSlugs(byTag, "batch21-phase3-admin-match");
    Map<?, ?> taggedArticle = first(byTag);
    assertThat(
            ((List<?>) taggedArticle.get("tags"))
                .stream()
                .map(tag -> ((Map<?, ?>) tag).get("name"))
                .toList())
        .isEqualTo(List.of("Batch21 Phase3 Tag A", "Batch21 Phase3 Tag B"));
    Map<?, ?> category = (Map<?, ?>) taggedArticle.get("category");
    assertThat(((Number) category.get("id")).longValue()).isEqualTo(categoryId);
    assertThat(category.get("name")).isEqualTo("Batch21 Phase3 Category");
    assertThat(category.get("url")).isEqualTo("/category/batch21-phase3-category");
    assertThat(
            taggedArticle
                .keySet()
                .containsAll(
                    List.of(
                        "id",
                        "title",
                        "slug",
                        "url",
                        "content",
                        "content_markdown",
                        "summary",
                        "cover",
                        "is_publish",
                        "update_time")))
        .isTrue();
    assertThat(
            java.util.Collections.disjoint(
                taggedArticle.keySet(), List.of("status", "category_id", "category_name", "category_slug")))
        .isTrue();

    assertArticleSlugs(adminGet("/api/v1/admin/articles?location=Batch21City", headers), "batch21-phase3-admin-match");
    assertArticleSlugs(adminGet("/api/v1/admin/articles?is_top=true&keyword=batch21-phase3-admin", headers), "batch21-phase3-admin-match");
    assertArticleSlugs(adminGet("/api/v1/admin/articles?is_essence=true&keyword=batch21-phase3-admin", headers), "batch21-phase3-admin-match");
    assertArticleSlugs(adminGet("/api/v1/admin/articles?is_outdated=true&keyword=batch21-phase3-admin", headers), "batch21-phase3-admin-match");
    assertArticleSlugs(
        adminGet("/api/v1/admin/articles?is_publish=true&keyword=batch21-phase3-admin", headers),
        "batch21-phase3-admin-match",
        "batch21-phase3-admin-other");
    assertArticleSlugs(
        adminGet(
            "/api/v1/admin/articles?start_time="
                + LocalDate.now()
                + "&end_time="
                + LocalDate.now()
                + "&keyword=batch21-phase3-admin",
            headers),
        "batch21-phase3-admin-match",
        "batch21-phase3-admin-other");

    Map<?, ?> paged = adminGet("/api/v1/admin/articles?keyword=batch21-phase3-admin&page=1&page_size=1", headers);
    assertPageShape(paged, 1, 1);
    assertThat(((Number) paged.get("total")).longValue()).isEqualTo(2);
    assertThat((List<?>) paged.get("list")).hasSize(1);
    assertThat(first(paged).get("slug")).isEqualTo("batch21-phase3-admin-match");
    assertThat(first(paged).get("publish_time").toString()).contains("T").endsWith("Z");
    assertThat(first(paged).get("update_time").toString()).contains("T").endsWith("Z");
  }

  @Test
  void adminArticleListLoadsTagsInBulkForPageArticles() {
    HttpHeaders headers = authenticatedHeaders();
    long categoryId = insertCategory("Batch21 Mybatis Bulk Category", "batch21-phase3-bulk-category");
    long tagA = insertTag("Batch21 Mybatis Bulk Tag A", "batch21-phase3-bulk-tag-a");
    long tagB = insertTag("Batch21 Mybatis Bulk Tag B", "batch21-phase3-bulk-tag-b");
    insertArticle(
        "batch21-phase3-bulk-a",
        "Batch21 Mybatis Bulk A",
        true,
        false,
        false,
        false,
        categoryId,
        List.of(tagA),
        "BulkCity",
        LocalDate.now().atTime(10, 0));
    insertArticle(
        "batch21-phase3-bulk-b",
        "Batch21 Mybatis Bulk B",
        true,
        false,
        false,
        false,
        categoryId,
        List.of(tagB),
        "BulkCity",
        LocalDate.now().atTime(9, 0));

    Map<?, ?> page = adminGet("/api/v1/admin/articles?location=BulkCity&page_size=20", headers);
    assertArticleSlugs(page, "batch21-phase3-bulk-a", "batch21-phase3-bulk-b");
    assertThat((List<?>) first(page).get("tags")).hasSize(1);
    Class<?> repositoryClass = org.springframework.aop.support.AopUtils.getTargetClass(articleAdminQueryRepository);
    assertThat(
            java.util.Arrays.stream(repositoryClass.getDeclaredMethods())
                .map(java.lang.reflect.Method::getName)
                .toList())
        .contains("findTagsByArticleIds")
        .doesNotContain("findTags");
  }

  private HttpHeaders authenticatedHeaders() {
    ResponseEntity<Map> loginResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/login",
            Map.of("username", "admin", "password", "admin123456"),
            Map.class);
    String accessToken = (String) ((Map<?, ?>) data(loginResponse)).get("access_token");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    return headers;
  }

  private Map<?, ?> adminGet(String path, HttpHeaders headers) {
    ResponseEntity<Map> response =
        restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return (Map<?, ?>) data(response);
  }

  private void assertPageShape(Map<?, ?> page, int expectedPage, int expectedPageSize) {
    assertThat(page.keySet().containsAll(List.of("list", "total", "page", "page_size"))).isTrue();
    assertThat(((Number) page.get("page")).intValue()).isEqualTo(expectedPage);
    assertThat(((Number) page.get("page_size")).intValue()).isEqualTo(expectedPageSize);
    assertThat(((Number) page.get("total")).longValue()).isGreaterThanOrEqualTo(0);
  }

  private void assertArticleSlugs(Map<?, ?> page, String... expectedSlugs) {
    assertThat(
            ((List<?>) page.get("list"))
                .stream()
                .map(row -> ((Map<?, ?>) row).get("slug"))
                .toList())
        .isEqualTo(List.of(expectedSlugs));
  }

  private Map<?, ?> first(Map<?, ?> page) {
    return (Map<?, ?>) ((List<?>) page.get("list")).getFirst();
  }

  private Object data(ResponseEntity<Map> response) {
    Map<?, ?> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("code")).isEqualTo(0);
    return body.get("data");
  }

  private long insertCategory(String name, String slug) {
    return insert("insert into categories (name, slug, description) values (?, ?, '')", name, slug);
  }

  private long insertTag(String name, String slug) {
    return insert("insert into tags (name, slug, description) values (?, ?, '')", name, slug);
  }

  private long insertArticle(
      String slug,
      String title,
      boolean published,
      boolean top,
      boolean essence,
      boolean outdated,
      Long categoryId,
      List<Long> tagIds,
      String location,
      LocalDateTime publishedAt) {
    return insertArticleWithContent(
        slug,
        title,
        title + " summary",
        "# " + title,
        "<h1>" + title + "</h1>",
        title + " content",
        published,
        categoryId,
        tagIds,
        location,
        publishedAt,
        top,
        essence,
        outdated);
  }

  private long insertArticleWithContent(
      String slug,
      String title,
      String summary,
      String markdown,
      String html,
      String text,
      boolean published,
      Long categoryId,
      List<Long> tagIds,
      String location,
      LocalDateTime publishedAt) {
    return insertArticleWithContent(
        slug,
        title,
        summary,
        markdown,
        html,
        text,
        published,
        categoryId,
        tagIds,
        location,
        publishedAt,
        false,
        false,
        false);
  }

  private long insertArticleWithContent(
      String slug,
      String title,
      String summary,
      String markdown,
      String html,
      String text,
      boolean published,
      Long categoryId,
      List<Long> tagIds,
      String location,
      LocalDateTime publishedAt,
      boolean top,
      boolean essence,
      boolean outdated) {
    long id =
        insert(
            """
            insert into articles (
              title, slug, summary, content_markdown, content_html, content_text,
              status, category_id, is_top, is_essence, is_outdated, location, published_at, updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            title,
            slug,
            summary,
            markdown,
            html,
            text,
            published ? "PUBLISHED" : "DRAFT",
            categoryId,
            top,
            essence,
            outdated,
            location,
            java.sql.Timestamp.valueOf(publishedAt),
            java.sql.Timestamp.valueOf(publishedAt));
    for (Long tagId : tagIds) {
      jdbcTemplate.update("insert into article_tags (article_id, tag_id) values (?, ?)", id, tagId);
    }
    return id;
  }

  private long insert(String sql, Object... args) {
    org.springframework.jdbc.support.KeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          java.sql.PreparedStatement statement = connection.prepareStatement(sql, new String[] {"id"});
          for (int i = 0; i < args.length; i++) {
            statement.setObject(i + 1, args[i]);
          }
          return statement;
        },
        keyHolder);
    return keyHolder.getKey().longValue();
  }
}
