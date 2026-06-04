package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.zblog.common.api.PageResponse;
import com.zblog.content.application.port.ArticleHotArticleRepository;
import com.zblog.content.application.port.ArticleSearchProjectionRepository;
import com.zblog.content.domain.ArticleSearchProjection;
import com.zblog.search.application.port.ArticleSearchSource;
import com.zblog.search.domain.SearchDocument;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Batch21Phase3RemainingContentQueryMyBatisTest {

  @Autowired private ArticleHotArticleRepository articleHotArticleRepository;

  @Autowired private ArticleSearchProjectionRepository articleSearchProjectionRepository;

  @Autowired private ArticleSearchSource articleSearchSource;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void hotAndSearchRepositoriesUseMyBatisInfrastructureAdapters() {
    assertThat(articleHotArticleRepository.getClass().getName())
        .contains("com.zblog.content.infrastructure.mybatis.MyBatisArticleHotArticleRepository");
    assertThat(articleSearchProjectionRepository.getClass().getName())
        .contains("com.zblog.content.infrastructure.mybatis.MyBatisArticleSearchProjectionRepository");
    assertThat(articleSearchSource.getClass().getName())
        .contains("com.zblog.content.infrastructure.mybatis.MyBatisArticleSearchProjectionRepository");
  }

  @Test
  void hotPublishedKeepsPublishedSortingLimitRelationsAndBulkTags() {
    long categoryId = insertCategory("Batch21 Remaining Hot Category", "batch21-remaining-hot-category");
    long tagA = insertTag("Batch21 Remaining Hot Tag A", "batch21-remaining-hot-tag-a");
    long tagB = insertTag("Batch21 Remaining Hot Tag B", "batch21-remaining-hot-tag-b");
    insertArticle(
        "batch21-remaining-hot-low",
        "Batch21 Remaining Hot Low",
        true,
        categoryId,
        List.of(tagA),
        10,
        LocalDate.of(2026, 3, 1).atTime(10, 0));
    insertArticle(
        "batch21-remaining-hot-top-old",
        "Batch21 Remaining Hot Top Old",
        true,
        categoryId,
        List.of(tagA, tagB),
        99,
        LocalDate.of(2026, 3, 1).atTime(9, 0));
    insertArticle(
        "batch21-remaining-hot-top-new",
        "Batch21 Remaining Hot Top New",
        true,
        categoryId,
        List.of(tagB),
        99,
        LocalDate.of(2026, 3, 2).atTime(9, 0));
    insertArticle(
        "batch21-remaining-hot-draft",
        "Batch21 Remaining Hot Draft",
        false,
        categoryId,
        List.of(tagA),
        999,
        LocalDate.of(2026, 3, 3).atTime(9, 0));

    List<Map<String, Object>> articles = articleHotArticleRepository.hotPublished(2);

    assertThat(articles.stream().map(row -> row.get("slug")).toList())
        .isEqualTo(List.of("batch21-remaining-hot-top-new", "batch21-remaining-hot-top-old"));
    Map<String, Object> first = articles.getFirst();
    assertThat(first.get("is_publish")).isEqualTo(true);
    assertThat(first.get("publish_time").toString()).contains("T").endsWith("Z");
    assertThat(first.get("update_time").toString()).contains("T").endsWith("Z");
    assertThat(first.keySet().containsAll(List.of("category", "tags", "content_markdown", "view_count")))
        .isTrue();
    assertThat(
            java.util.Collections.disjoint(
                first.keySet(), List.of("status", "category_id", "category_name", "category_slug")))
        .isTrue();
    assertThat((List<?>) articles.get(1).get("tags")).hasSize(2);
    assertBulkTagMethod(articleHotArticleRepository);
  }

  @Test
  void findPublishedByIdsSkipsMissingAndDraftAndRestoresInputOrder() {
    long tagId = insertTag("Batch21 Remaining Id Tag", "batch21-remaining-id-tag");
    long first =
        insertArticle(
            "batch21-remaining-id-first",
            "Batch21 Remaining Id First",
            true,
            null,
            List.of(tagId),
            1,
            LocalDate.of(2026, 2, 1).atTime(10, 0));
    long second =
        insertArticle(
            "batch21-remaining-id-second",
            "Batch21 Remaining Id Second",
            true,
            null,
            List.of(tagId),
            2,
            LocalDate.of(2026, 2, 2).atTime(10, 0));
    long draft =
        insertArticle(
            "batch21-remaining-id-draft",
            "Batch21 Remaining Id Draft",
            false,
            null,
            List.of(tagId),
            3,
            LocalDate.of(2026, 2, 3).atTime(10, 0));

    assertThat(articleHotArticleRepository.findPublishedByIds(null)).isEmpty();
    assertThat(articleHotArticleRepository.findPublishedByIds(List.of())).isEmpty();

    List<Map<String, Object>> articles =
        articleHotArticleRepository.findPublishedByIds(List.of(second, -999L, draft, first));

    assertThat(articles.stream().map(row -> row.get("slug")).toList())
        .isEqualTo(List.of("batch21-remaining-id-second", "batch21-remaining-id-first"));
    assertThat((List<?>) articles.getFirst().get("tags")).hasSize(1);
  }

  @Test
  void searchProjectionQueriesOnlyPublishedAndKeepSortAndInstantFormatting() {
    long draftId =
        insertArticleWithContent(
            "batch21-remaining-projection-draft",
            "Batch21 Remaining Projection Draft",
            "Draft summary",
            "# Draft",
            "<p>Draft</p>",
            "Draft text",
            false,
            null,
            List.of(),
            1,
            LocalDate.of(2026, 1, 3).atTime(10, 0));
    long olderId =
        insertArticleWithContent(
            "batch21-remaining-projection-older",
            "Batch21 Remaining Projection Older",
            "Older summary",
            "# Older",
            "<p>Older</p>",
            "Older text",
            true,
            null,
            List.of(),
            1,
            LocalDate.of(2026, 1, 1).atTime(10, 0));
    long newerId =
        insertArticleWithContent(
            "batch21-remaining-projection-newer",
            "Batch21 Remaining Projection Newer",
            "Newer summary",
            "# Newer",
            "<p>Newer</p>",
            "Newer text",
            true,
            null,
            List.of(),
            1,
            LocalDate.of(2026, 1, 2).atTime(10, 0));
    long nullTimeId =
        insertArticleWithContent(
            "batch21-remaining-projection-null-time",
            "Batch21 Remaining Projection Null Time",
            "Null time summary",
            "# Null time",
            "<p>Null time</p>",
            "Null time text",
            true,
            null,
            List.of(),
            1,
            null);

    Optional<ArticleSearchProjection> draftProjection =
        articleSearchProjectionRepository.publishedSearchProjection(draftId);
    Optional<ArticleSearchProjection> olderProjection =
        articleSearchProjectionRepository.publishedSearchProjection(olderId);
    Optional<ArticleSearchProjection> nullTimeProjection =
        articleSearchProjectionRepository.publishedSearchProjection(nullTimeId);

    assertThat(draftProjection).isEmpty();
    assertThat(olderProjection).isPresent();
    assertThat(olderProjection.get().publishedAt()).contains("T").endsWith("Z");
    assertThat(nullTimeProjection).isPresent();
    assertThat(nullTimeProjection.get().publishedAt()).isNull();

    List<ArticleSearchProjection> projections = articleSearchProjectionRepository.publishedSearchProjections();
    assertThat(projections.stream().map(ArticleSearchProjection::articleId).toList())
        .containsSubsequence(newerId, olderId);

    List<SearchDocument> documents = articleSearchSource.publishedSearchDocuments();
    assertThat(documents.stream().map(SearchDocument::articleId).toList()).containsSubsequence(newerId, olderId);
    SearchDocument newerDocument =
        documents.stream().filter(document -> document.articleId() == newerId).findFirst().orElseThrow();
    assertThat(newerDocument.title()).isEqualTo("Batch21 Remaining Projection Newer");
    assertThat(newerDocument.contentText()).isEqualTo("Newer text");
    assertThat(newerDocument.publishedAt()).contains("T").endsWith("Z");
  }

  @Test
  void searchPublicKeepsKeywordSemanticsPagingRelationsAndBulkTags() {
    long categoryId = insertCategory("Batch21 Remaining Search Category", "batch21-remaining-search-category");
    long tagId = insertTag("Batch21 Remaining Search Tag", "batch21-remaining-search-tag");
    insertArticleWithContent(
        "batch21-remaining-search-title",
        "Batch21 search-visible-token title",
        "No matching summary",
        "# No matching markdown",
        "<p>No matching html</p>",
        "No matching text",
        true,
        categoryId,
        List.of(tagId),
        1,
        LocalDate.of(2026, 6, 1).atTime(10, 0));
    insertArticleWithContent(
        "batch21-remaining-search-summary",
        "No matching title",
        "search-visible-token summary",
        "# No matching markdown",
        "<p>No matching html</p>",
        "No matching text",
        true,
        categoryId,
        List.of(tagId),
        1,
        LocalDate.of(2026, 6, 2).atTime(10, 0));
    insertArticleWithContent(
        "batch21-remaining-search-text",
        "No matching title",
        "No matching summary",
        "# No matching markdown",
        "<p>No matching html</p>",
        "search-visible-token text",
        true,
        categoryId,
        List.of(tagId),
        1,
        LocalDate.of(2026, 6, 3).atTime(10, 0));
    insertArticleWithContent(
        "batch21-remaining-search-markdown-only",
        "No matching title",
        "No matching summary",
        "# markdown-html-only-token",
        "<p>markdown-html-only-token</p>",
        "No matching text",
        true,
        categoryId,
        List.of(tagId),
        1,
        LocalDate.of(2026, 6, 4).atTime(10, 0));
    insertArticleWithContent(
        "batch21-remaining-search-draft",
        "search-visible-token draft",
        "search-visible-token draft summary",
        "# search-visible-token draft",
        "<p>search-visible-token draft</p>",
        "search-visible-token draft text",
        false,
        categoryId,
        List.of(tagId),
        1,
        LocalDate.of(2026, 6, 5).atTime(10, 0));

    PageResponse<Map<String, Object>> result = articleSearchSource.searchPublic(" search-visible-token ", 1, 2);

    assertThat(result.total()).isEqualTo(3);
    assertThat(result.page()).isEqualTo(1);
    assertThat(result.pageSize()).isEqualTo(2);
    assertThat(result.list().stream().map(row -> row.get("slug")).toList())
        .isEqualTo(List.of("batch21-remaining-search-text", "batch21-remaining-search-summary"));
    Map<String, Object> first = result.list().getFirst();
    assertThat(first.get("is_publish")).isEqualTo(true);
    assertThat(first.get("publish_time").toString()).contains("T").endsWith("Z");
    assertThat(first.get("update_time").toString()).contains("T").endsWith("Z");
    assertThat((List<?>) first.get("tags")).hasSize(1);
    assertThat(((Map<?, ?>) first.get("category")).get("url"))
        .isEqualTo("/category/batch21-remaining-search-category");
    assertThat(
            java.util.Collections.disjoint(
                first.keySet(), List.of("status", "category_id", "category_name", "category_slug")))
        .isTrue();

    assertThat(articleSearchSource.searchPublic("markdown-html-only-token", 1, 10).list()).isEmpty();
    assertThat(articleSearchSource.searchPublic("   ", 1, 10).list()).isNotEmpty();
    assertBulkTagMethod(articleSearchSource);
  }

  private void assertBulkTagMethod(Object repository) {
    Class<?> repositoryClass = org.springframework.aop.support.AopUtils.getTargetClass(repository);
    assertThat(
            java.util.Arrays.stream(repositoryClass.getDeclaredMethods())
                .map(java.lang.reflect.Method::getName)
                .toList())
        .contains("findTagsByArticleIds")
        .doesNotContain("findTags");
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
      Long categoryId,
      List<Long> tagIds,
      long viewCount,
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
        viewCount,
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
      Long categoryId,
      List<Long> tagIds,
      long viewCount,
      LocalDateTime publishedAt) {
    long id =
        insert(
            """
            insert into articles (
              title, slug, summary, content_markdown, content_html, content_text,
              status, category_id, view_count, published_at, updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            title,
            slug,
            summary,
            markdown,
            html,
            text,
            published ? "PUBLISHED" : "DRAFT",
            categoryId,
            viewCount,
            publishedAt == null ? null : java.sql.Timestamp.valueOf(publishedAt),
            publishedAt == null ? java.sql.Timestamp.valueOf(LocalDate.of(2026, 1, 1).atStartOfDay()) : java.sql.Timestamp.valueOf(publishedAt));
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
