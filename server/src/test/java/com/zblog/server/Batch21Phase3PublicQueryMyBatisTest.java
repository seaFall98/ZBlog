package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.content.application.port.ArticlePublicQueryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Batch21Phase3PublicQueryMyBatisTest {

  @Autowired private ArticlePublicQueryRepository articlePublicQueryRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void articlePublicQueryRepositoryUsesMyBatisInfrastructureAdapter() {
    assertThat(articlePublicQueryRepository.getClass().getName())
        .contains("com.zblog.content.infrastructure.mybatis.MyBatisArticlePublicQueryRepository");
  }

  @Test
  void publicArticleListKeepsPagingSortingFiltersAndRelationsContract() {
    long categoryId = insertCategory("Batch21 Public Category", "batch21-public-category");
    long otherCategoryId = insertCategory("Batch21 Other Category", "batch21-other-category");
    long tagA = insertTag("Batch21 Public Tag A", "batch21-public-tag-a");
    long tagB = insertTag("Batch21 Public Tag B", "batch21-public-tag-b");
    LocalDateTime newest = LocalDate.of(2026, 4, 20).atTime(10, 0);
    LocalDateTime older = LocalDate.of(2026, 4, 10).atTime(9, 0);
    insertArticle(
        "batch21-public-top",
        "Batch21 Public Top",
        true,
        true,
        categoryId,
        List.of(tagA, tagB),
        "PublicCity",
        older);
    insertArticle(
        "batch21-public-newest",
        "Batch21 Public Newest",
        true,
        false,
        categoryId,
        List.of(tagA),
        "PublicCity",
        newest);
    insertArticle(
        "batch21-public-other-category",
        "Batch21 Public Other Category",
        true,
        false,
        otherCategoryId,
        List.of(tagB),
        "PublicCity",
        newest.plusHours(1));
    insertArticle(
        "batch21-public-draft",
        "Batch21 Public Draft",
        false,
        true,
        categoryId,
        List.of(tagA),
        "PublicCity",
        newest.plusDays(1));

    Map<?, ?> page = publicGet("/api/v1/articles?category=batch21-public-category&tag=batch21-public-tag-a&year=2026&month=4&page=1&page_size=10");
    assertPageShape(page, 1, 10);
    assertArticleSlugs(page, "batch21-public-top", "batch21-public-newest");

    Map<?, ?> first = first(page);
    assertThat(first.get("is_publish")).isEqualTo(true);
    assertThat(first.get("content_markdown")).isEqualTo("# Batch21 Public Top");
    assertThat(first.get("publish_time").toString()).contains("T").endsWith("Z");
    assertThat(first.get("update_time").toString()).contains("T").endsWith("Z");
    assertThat(first.keySet().containsAll(List.of("category", "tags", "is_publish", "content_markdown")))
        .isTrue();
    assertThat(
            java.util.Collections.disjoint(
                first.keySet(), List.of("status", "category_id", "category_name", "category_slug")))
        .isTrue();
    Map<?, ?> category = (Map<?, ?>) first.get("category");
    assertThat(((Number) category.get("id")).longValue()).isEqualTo(categoryId);
    assertThat(category.get("name")).isEqualTo("Batch21 Public Category");
    assertThat(category.get("url")).isEqualTo("/category/batch21-public-category");
    assertThat(
            ((List<?>) first.get("tags"))
                .stream()
                .map(tag -> ((Map<?, ?>) tag).get("name"))
                .toList())
        .isEqualTo(List.of("Batch21 Public Tag A", "Batch21 Public Tag B"));

    Map<?, ?> paged = publicGet("/api/v1/articles?category=batch21-public-category&tag=batch21-public-tag-a&year=2026&month=4&page=1&page_size=1");
    assertPageShape(paged, 1, 1);
    assertThat(((Number) paged.get("total")).longValue()).isEqualTo(2);
    assertArticleSlugs(paged, "batch21-public-top");
  }

  @Test
  void publicArticleDetailAndRandomOnlyExposePublishedArticles() {
    long categoryId = insertCategory("Batch21 Public Detail Category", "batch21-public-detail-category");
    long tagId = insertTag("Batch21 Public Detail Tag", "batch21-public-detail-tag");
    insertArticle(
        "batch21-public-detail-draft",
        "Batch21 Public Detail Draft",
        false,
        false,
        categoryId,
        List.of(tagId),
        "DetailCity",
        LocalDate.now().atTime(8, 0));
    long publishedId =
        insertArticle(
            "batch21-public-detail-published",
            "Batch21 Public Detail Published",
            true,
            false,
            categoryId,
            List.of(tagId),
            "DetailCity",
            LocalDate.now().atTime(9, 0));

    Map<?, ?> article = publicGet("/api/v1/articles/batch21-public-detail-published");
    assertThat(article.get("slug")).isEqualTo("batch21-public-detail-published");
    assertThat(article.get("is_publish")).isEqualTo(true);
    assertThat((List<?>) article.get("tags")).hasSize(1);

    ResponseEntity<Map> draftResponse =
        restTemplate.getForEntity("/api/v1/articles/batch21-public-detail-draft", Map.class);
    assertThat(draftResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(draftResponse.getBody()).isNotNull();
    assertThat(draftResponse.getBody().get("message")).isEqualTo("Article not found");

    List<Map<String, Object>> originalStatuses = jdbcTemplate.queryForList("select id, status from articles");
    try {
      jdbcTemplate.update("update articles set status = 'DRAFT' where id <> ?", publishedId);
      assertThat(articlePublicQueryRepository.randomPublishedSlug()).isEqualTo("batch21-public-detail-published");
      jdbcTemplate.update("update articles set status = 'DRAFT' where slug = ?", "batch21-public-detail-published");
      assertThatThrownBy(() -> articlePublicQueryRepository.randomPublishedSlug())
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("Article not found");
    } finally {
      for (Map<String, Object> row : originalStatuses) {
        jdbcTemplate.update("update articles set status = ? where id = ?", row.get("status"), row.get("id"));
      }
    }
  }

  @Test
  void searchPublicOnlyMatchesTitleSummaryAndContentTextAndKeepsRelations() {
    long categoryId = insertCategory("Batch21 Public Search Category", "batch21-public-search-category");
    long tagId = insertTag("Batch21 Public Search Tag", "batch21-public-search-tag");
    insertArticleWithContent(
        "batch21-public-search-title",
        "Batch21 Search visible-token",
        "No matching summary",
        "# No matching markdown",
        "<p>No matching html</p>",
        "No matching text",
        true,
        false,
        categoryId,
        List.of(tagId),
        "SearchCity",
        LocalDate.of(2026, 5, 1).atTime(10, 0));
    insertArticleWithContent(
        "batch21-public-search-summary",
        "No matching title",
        "visible-token in summary",
        "# No matching markdown",
        "<p>No matching html</p>",
        "No matching text",
        true,
        false,
        categoryId,
        List.of(tagId),
        "SearchCity",
        LocalDate.of(2026, 5, 2).atTime(10, 0));
    insertArticleWithContent(
        "batch21-public-search-text",
        "No matching title",
        "No matching summary",
        "# No matching markdown",
        "<p>No matching html</p>",
        "visible-token in text",
        true,
        false,
        categoryId,
        List.of(tagId),
        "SearchCity",
        LocalDate.of(2026, 5, 3).atTime(10, 0));
    insertArticleWithContent(
        "batch21-public-search-markdown-only",
        "No matching title",
        "No matching summary",
        "# markdown-html-only-token",
        "<p>markdown-html-only-token</p>",
        "No matching text",
        true,
        false,
        categoryId,
        List.of(tagId),
        "SearchCity",
        LocalDate.of(2026, 5, 4).atTime(10, 0));
    insertArticleWithContent(
        "batch21-public-search-draft",
        "visible-token draft",
        "visible-token draft summary",
        "# visible-token draft",
        "<p>visible-token draft</p>",
        "visible-token draft text",
        false,
        false,
        categoryId,
        List.of(tagId),
        "SearchCity",
        LocalDate.of(2026, 5, 5).atTime(10, 0));

    PageResponse<Map<String, Object>> result = articlePublicQueryRepository.searchPublic("visible-token", 1, 10);
    assertThat(result.total()).isEqualTo(3);
    assertThat(result.list().stream().map(row -> row.get("slug")).toList())
        .isEqualTo(
            List.of(
                "batch21-public-search-text",
                "batch21-public-search-summary",
                "batch21-public-search-title"));
    Map<String, Object> first = result.list().getFirst();
    assertThat(first.get("publish_time").toString()).contains("T").endsWith("Z");
    assertThat((List<?>) first.get("tags")).hasSize(1);

    PageResponse<Map<String, Object>> markdownOnly =
        articlePublicQueryRepository.searchPublic("markdown-html-only-token", 1, 10);
    assertThat(markdownOnly.list()).isEmpty();
    assertThat(markdownOnly.total()).isZero();
  }

  @Test
  void publicArticleListLoadsTagsInBulkForPageArticles() {
    long tagA = insertTag("Batch21 Public Bulk Tag A", "batch21-public-bulk-tag-a");
    long tagB = insertTag("Batch21 Public Bulk Tag B", "batch21-public-bulk-tag-b");
    insertArticle(
        "batch21-public-bulk-a",
        "Batch21 Public Bulk A",
        true,
        false,
        null,
        List.of(tagA),
        "BulkCity",
        LocalDate.now().atTime(10, 0));
    insertArticle(
        "batch21-public-bulk-b",
        "Batch21 Public Bulk B",
        true,
        false,
        null,
        List.of(tagB),
        "BulkCity",
        LocalDate.now().atTime(9, 0));

    PageResponse<Map<String, Object>> result = articlePublicQueryRepository.searchPublic("Batch21 Public Bulk", 1, 20);
    assertThat(result.list().stream().map(row -> row.get("slug")).toList())
        .isEqualTo(List.of("batch21-public-bulk-a", "batch21-public-bulk-b"));
    assertThat((List<?>) result.list().getFirst().get("tags")).hasSize(1);
    Class<?> repositoryClass = org.springframework.aop.support.AopUtils.getTargetClass(articlePublicQueryRepository);
    assertThat(
            java.util.Arrays.stream(repositoryClass.getDeclaredMethods())
                .map(java.lang.reflect.Method::getName)
                .toList())
        .contains("findTagsByArticleIds")
        .doesNotContain("findTags");
  }

  private Map<?, ?> publicGet(String path) {
    ResponseEntity<Map> response = restTemplate.getForEntity(path, Map.class);
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
        top,
        categoryId,
        tagIds,
        location,
        publishedAt);
  }

  private long insertArticleWithContent(
      String slug,
      String title,
      String summary,
      String markdown,
      String html,
      String text,
      boolean published,
      boolean top,
      Long categoryId,
      List<Long> tagIds,
      String location,
      LocalDateTime publishedAt) {
    long id =
        insert(
            """
            insert into articles (
              title, slug, summary, content_markdown, content_html, content_text,
              status, category_id, is_top, is_essence, is_outdated, location, published_at, updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, false, false, ?, ?, ?)
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
