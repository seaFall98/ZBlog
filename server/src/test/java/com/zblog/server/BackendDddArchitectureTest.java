package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class BackendDddArchitectureTest {

  private static final Path SOURCE_ROOT = Path.of("src/main/java/com/zblog");

  private static final Set<String> DOMAIN_FORBIDDEN_IMPORTS =
      Set.of(
          "org.springframework",
          "jakarta.servlet",
          "javax.servlet",
          "java.sql",
          "org.springframework.jdbc",
          "org.springframework.data.redis",
          "org.springframework.amqp",
          "org.elasticsearch");

  private static final Set<String> APPLICATION_INFRASTRUCTURE_IMPORT_ALLOWLIST = Set.of();

  private static final Set<String> APPLICATION_PERSISTENCE_FORBIDDEN_IMPORTS =
      Set.of("org.springframework.jdbc", "java.sql", "org.apache.ibatis", "org.mybatis", "com.baomidou");

  private static final Set<String> CONTROLLER_PERSISTENCE_FORBIDDEN_IMPORTS =
      Set.of("org.springframework.jdbc", "java.sql", "org.apache.ibatis", "org.mybatis", "com.baomidou");

  private static final Set<String> MYBATIS_FORBIDDEN_OUTSIDE_INFRASTRUCTURE =
      Set.of("org.apache.ibatis", "org.mybatis");

  private static final Set<String> MYBATIS_PLUS_FORBIDDEN_IMPORTS = Set.of("com.baomidou");

  private static final Set<String> OBSOLETE_BATCH21_JDBC_CLASSES =
      Set.of(
          "com.zblog.content.infrastructure.JdbcArticleAdminQueryRepository",
          "com.zblog.content.infrastructure.JdbcArticlePublicQueryRepository",
          "com.zblog.content.infrastructure.JdbcArticleHotArticleRepository",
          "com.zblog.content.infrastructure.JdbcArticleSearchProjectionRepository",
          "com.zblog.content.infrastructure.JdbcArticleCommandRepository",
          "com.zblog.content.infrastructure.JdbcArticleViewSupport",
          "com.zblog.stats.infrastructure.JdbcStatsRepository",
          "com.zblog.stats.infrastructure.JdbcVisitRepository",
          "com.zblog.seo.infrastructure.JdbcSeoFeedRepository",
          "com.zblog.search.infrastructure.jdbc.JdbcSearchStatusRepository");

  private static final Set<String> RETAINED_JDBC_CLASSES = Set.of();

  @Test
  void domainPackagesStayFreeOfFrameworkAndAdapterImports() throws IOException {
    List<String> violations = new ArrayList<>();

    for (Path source : javaSources()) {
      if (!source.toString().replace('\\', '/').contains("/domain/")) {
        continue;
      }

      List<String> imports = importLines(source);
      for (String importLine : imports) {
        for (String forbiddenImport : DOMAIN_FORBIDDEN_IMPORTS) {
          if (importLine.startsWith("import " + forbiddenImport)) {
            violations.add(relative(source) + " imports " + importLine);
          }
        }
      }
    }

    assertThat(violations).isEmpty();
  }

  @Test
  void controllersDoNotImportPersistenceImplementations() throws IOException {
    List<String> violations = new ArrayList<>();

    for (Path source : javaSources()) {
      if (!source.toString().replace('\\', '/').contains("/controller/")) {
        continue;
      }

      for (String importLine : importLines(source)) {
        if (importLine.startsWith("import com.zblog.") && importLine.contains(".infrastructure.")) {
          violations.add(relative(source) + " imports " + importLine);
        }
        for (String forbiddenImport : CONTROLLER_PERSISTENCE_FORBIDDEN_IMPORTS) {
          if (importLine.startsWith("import " + forbiddenImport)) {
            violations.add(relative(source) + " imports " + importLine);
          }
        }
      }
    }

    assertThat(violations).isEmpty();
  }

  @Test
  void applicationLayerDoesNotImportPersistenceApis() throws IOException {
    List<String> violations = new ArrayList<>();

    for (Path source : javaSources()) {
      String path = source.toString().replace('\\', '/');
      if (!path.contains("/application/") || path.contains("/application/port/")) {
        continue;
      }

      for (String importLine : importLines(source)) {
        for (String forbiddenImport : APPLICATION_PERSISTENCE_FORBIDDEN_IMPORTS) {
          if (importLine.startsWith("import " + forbiddenImport)) {
            violations.add(relative(source) + " imports " + importLine);
          }
        }
      }
    }

    assertThat(violations).isEmpty();
  }

  @Test
  void mybatisDoesNotLeakOutsideInfrastructureAndMybatisPlusIsAbsent() throws IOException {
    List<String> violations = new ArrayList<>();

    for (Path source : javaSources()) {
      String path = source.toString().replace('\\', '/');
      for (String importLine : importLines(source)) {
        for (String forbiddenImport : MYBATIS_PLUS_FORBIDDEN_IMPORTS) {
          if (importLine.startsWith("import " + forbiddenImport)) {
            violations.add(relative(source) + " imports MyBatis-Plus " + importLine);
          }
        }
        if (path.contains("/infrastructure/")) {
          continue;
        }
        for (String forbiddenImport : MYBATIS_FORBIDDEN_OUTSIDE_INFRASTRUCTURE) {
          if (importLine.startsWith("import " + forbiddenImport)) {
            violations.add(relative(source) + " imports " + importLine);
          }
        }
      }
    }

    assertThat(violations).isEmpty();
  }

  @Test
  void applicationLayerDoesNotReachIntoOtherModuleInfrastructureWithoutAllowlist() throws IOException {
    List<String> violations = new ArrayList<>();

    for (Path source : javaSources()) {
      String path = source.toString().replace('\\', '/');
      if (!path.contains("/application/") || path.contains("/application/port/")) {
        continue;
      }

      String module = moduleName(source);
      for (String importLine : importLines(source)) {
        if (!importLine.startsWith("import com.zblog.") || !importLine.contains(".infrastructure.")) {
          continue;
        }

        String importedModule = importLine.replace("import com.zblog.", "").split("\\.")[0];
        if (module.equals(importedModule)) {
          continue;
        }

        String allowlistEntry = relative(source) + " -> " + importLine.replace("import ", "").replace(";", "");
        if (!APPLICATION_INFRASTRUCTURE_IMPORT_ALLOWLIST.contains(allowlistEntry)) {
          violations.add(allowlistEntry);
        }
      }
    }

    assertThat(violations).isEmpty();
  }

  @Test
  void contentApplicationServicesDependOnFocusedArticlePorts() throws Exception {
    assertConstructorParameterTypes(
        "com.zblog.content.application.ArticleService",
        "com.zblog.content.application.port.ArticleCommandRepository",
        "com.zblog.content.application.port.ArticleAdminQueryRepository");
    assertConstructorParameterTypes(
        "com.zblog.content.application.ArticleQueryService",
        "com.zblog.content.application.port.ArticlePublicQueryRepository",
        "com.zblog.content.application.port.ArticleAdminQueryRepository");
    assertConstructorParameterTypes(
        "com.zblog.content.application.ArticleHotRankingService",
        "com.zblog.content.application.port.ArticleHotArticleRepository");
    assertConstructorParameterTypes(
        "com.zblog.content.application.ArticleImportService",
        "com.zblog.content.application.port.ArticleCommandRepository",
        "com.zblog.content.application.port.ArticleImportSupportRepository");
    assertConstructorParameterTypes(
        "com.zblog.content.application.ArticleExportService",
        "com.zblog.content.application.port.ArticleAdminQueryRepository");
  }

  @Test
  void contentApplicationLayerDoesNotDependOnRemovedAggregateArticleRepository() throws IOException {
    List<String> violations = new ArrayList<>();

    for (Path source : javaSources()) {
      String path = source.toString().replace('\\', '/');
      if (!path.contains("/content/application/") || path.contains("/content/application/port/")) {
        continue;
      }
      for (String importLine : importLines(source)) {
        if (importLine.equals("import com.zblog.content.application.port.ArticleRepository;")) {
          violations.add(relative(source));
        }
      }
    }

    assertThat(violations).isEmpty();
  }

  @Test
  void articleWriteUseCasesHaveApplicationTransactionBoundary() throws Exception {
    Class<?> articleService = Class.forName("com.zblog.content.application.ArticleService");
    assertMethodTransactional(articleService, "create", Map.class);
    assertMethodTransactional(articleService, "update", long.class, Map.class);
    assertMethodTransactional(articleService, "publish", long.class);
    assertMethodTransactional(articleService, "unpublish", long.class);
    assertMethodTransactional(articleService, "delete", long.class);
  }

  @Test
  void searchIndexConsumerUsesEventConsumptionPortInsteadOfJdbcTemplate() throws Exception {
    assertConstructorParameterTypes(
        "com.zblog.search.infrastructure.messaging.SearchIndexEventConsumer",
        "com.zblog.event.application.port.EventConsumptionRepository");
    assertConstructorParameterTypesDoNotContain(
        "com.zblog.search.infrastructure.messaging.SearchIndexEventConsumer",
        "org.springframework.jdbc.core.JdbcTemplate");
  }

  @Test
  void contentInfrastructureUsesFocusedArticleAdapters() throws Exception {
    assertThat(Class.forName("com.zblog.content.infrastructure.mybatis.MyBatisArticleImportSupportRepository")).isNotNull();
    assertThat(Class.forName("com.zblog.content.infrastructure.mybatis.MyBatisArticleAdminQueryRepository")).isNotNull();
    assertThat(Class.forName("com.zblog.content.infrastructure.mybatis.MyBatisArticlePublicQueryRepository")).isNotNull();
    assertThat(Class.forName("com.zblog.content.infrastructure.mybatis.MyBatisArticleHotArticleRepository")).isNotNull();
    assertThat(Class.forName("com.zblog.content.infrastructure.mybatis.MyBatisArticleSearchProjectionRepository")).isNotNull();
    assertThat(Class.forName("com.zblog.content.infrastructure.mybatis.MyBatisArticleCommandRepository")).isNotNull();
    assertThat(classExists("com.zblog.content.infrastructure.JdbcArticleRepository")).isFalse();
    assertThat(classExists("com.zblog.content.application.port.ArticleRepository")).isFalse();
  }

  @Test
  void obsoleteBatch21JdbcAdaptersDoNotExist() {
    assertThat(OBSOLETE_BATCH21_JDBC_CLASSES).noneSatisfy(className -> assertThat(classExists(className)).isTrue());
  }

  @Test
  void noJdbcRepositoriesRemainAfterBatch22() throws IOException {
    Set<String> jdbcClasses =
        javaSources().stream()
            .filter(source -> source.getFileName().toString().startsWith("Jdbc"))
            .map(source -> "com.zblog." + SOURCE_ROOT.relativize(source).toString().replace('\\', '.').replace(".java", ""))
            .collect(java.util.stream.Collectors.toSet());

    assertThat(jdbcClasses).isEmpty();
  }

  private static void assertConstructorParameterTypes(String className, String... expectedTypes)
      throws ClassNotFoundException {
    Set<String> parameterTypes = constructorParameterTypes(className);

    assertThat(parameterTypes).contains(expectedTypes);
    assertThat(parameterTypes).doesNotContain("com.zblog.content.application.port.ArticleRepository");
  }

  private static void assertConstructorParameterTypesDoNotContain(String className, String... forbiddenTypes)
      throws ClassNotFoundException {
    assertThat(constructorParameterTypes(className)).doesNotContain(forbiddenTypes);
  }

  private static Set<String> constructorParameterTypes(String className) throws ClassNotFoundException {
    return Arrays.stream(Class.forName(className).getDeclaredConstructors())
        .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
        .map(Class::getName)
        .collect(java.util.stream.Collectors.toSet());
  }

  private static void assertMethodTransactional(Class<?> type, String methodName, Class<?>... parameterTypes)
      throws NoSuchMethodException {
    Method method = type.getDeclaredMethod(methodName, parameterTypes);
    assertThat(method.isAnnotationPresent(Transactional.class) || type.isAnnotationPresent(Transactional.class))
        .as(type.getName() + "#" + methodName + " should be protected by application transaction")
        .isTrue();
  }

  private static boolean classExists(String className) {
    try {
      Class.forName(className);
      return true;
    } catch (ClassNotFoundException exception) {
      return false;
    }
  }

  private static List<Path> javaSources() throws IOException {
    try (Stream<Path> paths = Files.walk(SOURCE_ROOT)) {
      return paths.filter(path -> path.toString().endsWith(".java")).toList();
    }
  }

  private static List<String> importLines(Path source) throws IOException {
    return Files.readAllLines(source).stream().map(String::trim).filter(line -> line.startsWith("import ")).toList();
  }

  private static String moduleName(Path source) {
    Path relative = SOURCE_ROOT.relativize(source);
    return relative.getName(0).toString();
  }

  private static String relative(Path source) {
    return SOURCE_ROOT.relativize(source).toString().replace('\\', '/');
  }
}
