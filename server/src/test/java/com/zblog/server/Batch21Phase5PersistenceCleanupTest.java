package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.zblog.content.application.port.ArticleImportSupportRepository;
import com.zblog.stats.application.port.StatsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Batch21Phase5PersistenceCleanupTest {

  @Autowired private ArticleImportSupportRepository articleImportSupportRepository;

  @Autowired private StatsRepository statsRepository;

  @Test
  void obsoleteBatch21JdbcImplementationsAreDeleted() {
    assertThat(classExists("com.zblog.content.infrastructure.JdbcArticleAdminQueryRepository")).isFalse();
    assertThat(classExists("com.zblog.content.infrastructure.JdbcArticlePublicQueryRepository")).isFalse();
    assertThat(classExists("com.zblog.content.infrastructure.JdbcArticleHotArticleRepository")).isFalse();
    assertThat(classExists("com.zblog.content.infrastructure.JdbcArticleSearchProjectionRepository")).isFalse();
    assertThat(classExists("com.zblog.content.infrastructure.JdbcArticleCommandRepository")).isFalse();
    assertThat(classExists("com.zblog.content.infrastructure.JdbcArticleViewSupport")).isFalse();
    assertThat(classExists("com.zblog.stats.infrastructure.JdbcStatsRepository")).isFalse();
  }

  @Test
  void retainedJdbcImplementationsRemainIntentionalAndActiveWhereRequired() {
    assertThat(classExists("com.zblog.content.infrastructure.JdbcArticleImportSupportRepository")).isTrue();
    assertThat(classExists("com.zblog.ops.infrastructure.JdbcSystemDatabaseInfoRepository")).isTrue();
    assertThat(AopUtils.getTargetClass(articleImportSupportRepository).getName())
        .isEqualTo("com.zblog.content.infrastructure.JdbcArticleImportSupportRepository");
  }

  @Test
  void statsRepositoryRemainsBackedByMyBatis() {
    assertThat(AopUtils.getTargetClass(statsRepository).getName())
        .isEqualTo("com.zblog.stats.infrastructure.mybatis.MyBatisStatsRepository");
  }

  private boolean classExists(String className) {
    try {
      Class.forName(className);
      return true;
    } catch (ClassNotFoundException exception) {
      return false;
    }
  }
}
