package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.zblog.album.application.port.AlbumRepository;
import com.zblog.comment.application.port.CommentRepository;
import com.zblog.content.application.port.ArticleImportSupportRepository;
import com.zblog.event.application.port.EventConsumptionRepository;
import com.zblog.event.application.port.EventOutboxRepository;
import com.zblog.feedback.application.port.FeedbackRepository;
import com.zblog.friend.application.port.FriendRepository;
import com.zblog.guestbook.application.port.GuestbookRepository;
import com.zblog.identity.application.port.PasswordResetTokenRepository;
import com.zblog.identity.application.port.UserRepository;
import com.zblog.mail.application.port.MailOutboxRepository;
import com.zblog.media.application.port.FileRepository;
import com.zblog.moment.application.port.MomentRepository;
import com.zblog.notification.application.port.NotificationRepository;
import com.zblog.ops.application.port.SystemDatabaseInfoRepository;
import com.zblog.rssfeed.application.port.RssFeedRepository;
import com.zblog.site.application.port.MenuRepository;
import com.zblog.site.application.port.SettingRepository;
import com.zblog.subscription.application.port.SubscriberRepository;
import com.zblog.taxonomy.application.port.TaxonomyRepository;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Batch22RemainingMyBatisMigrationTest {

  private static final Map<String, String> EXPECTED_MYBATIS_TARGETS =
      Map.ofEntries(
          Map.entry(
              "AlbumRepository", "com.zblog.album.infrastructure.mybatis.MyBatisAlbumRepository"),
          Map.entry(
              "CommentRepository", "com.zblog.comment.infrastructure.mybatis.MyBatisCommentRepository"),
          Map.entry(
              "ArticleImportSupportRepository",
              "com.zblog.content.infrastructure.mybatis.MyBatisArticleImportSupportRepository"),
          Map.entry(
              "EventOutboxRepository",
              "com.zblog.event.infrastructure.mybatis.MyBatisEventOutboxRepository"),
          Map.entry(
              "EventConsumptionRepository",
              "com.zblog.event.infrastructure.mybatis.MyBatisEventConsumptionRepository"),
          Map.entry(
              "FeedbackRepository",
              "com.zblog.feedback.infrastructure.mybatis.MyBatisFeedbackRepository"),
          Map.entry(
              "FriendRepository", "com.zblog.friend.infrastructure.mybatis.MyBatisFriendRepository"),
          Map.entry(
              "GuestbookRepository",
              "com.zblog.guestbook.infrastructure.mybatis.MyBatisGuestbookRepository"),
          Map.entry(
              "PasswordResetTokenRepository",
              "com.zblog.identity.infrastructure.mybatis.MyBatisPasswordResetTokenRepository"),
          Map.entry(
              "UserRepository", "com.zblog.identity.infrastructure.mybatis.MyBatisUserRepository"),
          Map.entry(
              "MailOutboxRepository",
              "com.zblog.mail.infrastructure.mybatis.MyBatisMailOutboxRepository"),
          Map.entry("FileRepository", "com.zblog.media.infrastructure.mybatis.MyBatisFileRepository"),
          Map.entry(
              "MomentRepository", "com.zblog.moment.infrastructure.mybatis.MyBatisMomentRepository"),
          Map.entry(
              "NotificationRepository",
              "com.zblog.notification.infrastructure.mybatis.MyBatisNotificationRepository"),
          Map.entry(
              "SystemDatabaseInfoRepository",
              "com.zblog.ops.infrastructure.mybatis.MyBatisSystemDatabaseInfoRepository"),
          Map.entry(
              "RssFeedRepository",
              "com.zblog.rssfeed.infrastructure.mybatis.MyBatisRssFeedRepository"),
          Map.entry("MenuRepository", "com.zblog.site.infrastructure.mybatis.MyBatisMenuRepository"),
          Map.entry(
              "SettingRepository", "com.zblog.site.infrastructure.mybatis.MyBatisSettingRepository"),
          Map.entry(
              "SubscriberRepository",
              "com.zblog.subscription.infrastructure.mybatis.MyBatisSubscriberRepository"),
          Map.entry(
              "TaxonomyRepository",
              "com.zblog.taxonomy.infrastructure.mybatis.MyBatisTaxonomyRepository"));

  private static final Set<String> REPLACED_JDBC_CLASSES =
      Set.of(
          "com.zblog.album.infrastructure.JdbcAlbumRepository",
          "com.zblog.comment.infrastructure.JdbcCommentRepository",
          "com.zblog.content.infrastructure.JdbcArticleImportSupportRepository",
          "com.zblog.event.infrastructure.JdbcEventOutboxRepository",
          "com.zblog.event.infrastructure.JdbcEventConsumptionRepository",
          "com.zblog.feedback.infrastructure.JdbcFeedbackRepository",
          "com.zblog.friend.infrastructure.JdbcFriendRepository",
          "com.zblog.guestbook.infrastructure.JdbcGuestbookRepository",
          "com.zblog.identity.infrastructure.JdbcPasswordResetTokenRepository",
          "com.zblog.identity.infrastructure.JdbcUserRepository",
          "com.zblog.mail.infrastructure.JdbcMailOutboxRepository",
          "com.zblog.media.infrastructure.JdbcFileRepository",
          "com.zblog.moment.infrastructure.JdbcMomentRepository",
          "com.zblog.notification.infrastructure.JdbcNotificationRepository",
          "com.zblog.ops.infrastructure.JdbcSystemDatabaseInfoRepository",
          "com.zblog.rssfeed.infrastructure.JdbcRssFeedRepository",
          "com.zblog.site.infrastructure.JdbcMenuRepository",
          "com.zblog.site.infrastructure.JdbcSettingRepository",
          "com.zblog.subscription.infrastructure.JdbcSubscriberRepository",
          "com.zblog.taxonomy.infrastructure.JdbcTaxonomyRepository",
          "com.zblog.search.infrastructure.jdbc.DbSearchAdapter");

  @Autowired private AlbumRepository albumRepository;
  @Autowired private CommentRepository commentRepository;
  @Autowired private ArticleImportSupportRepository articleImportSupportRepository;
  @Autowired private EventOutboxRepository eventOutboxRepository;
  @Autowired private EventConsumptionRepository eventConsumptionRepository;
  @Autowired private FeedbackRepository feedbackRepository;
  @Autowired private FriendRepository friendRepository;
  @Autowired private GuestbookRepository guestbookRepository;
  @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private MailOutboxRepository mailOutboxRepository;
  @Autowired private FileRepository fileRepository;
  @Autowired private MomentRepository momentRepository;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private SystemDatabaseInfoRepository systemDatabaseInfoRepository;
  @Autowired private RssFeedRepository rssFeedRepository;
  @Autowired private MenuRepository menuRepository;
  @Autowired private SettingRepository settingRepository;
  @Autowired private SubscriberRepository subscriberRepository;
  @Autowired private TaxonomyRepository taxonomyRepository;

  @Test
  void remainingPersistencePortsUseMyBatisAdapters() {
    assertTarget("AlbumRepository", albumRepository);
    assertTarget("CommentRepository", commentRepository);
    assertTarget("ArticleImportSupportRepository", articleImportSupportRepository);
    assertTarget("EventOutboxRepository", eventOutboxRepository);
    assertTarget("EventConsumptionRepository", eventConsumptionRepository);
    assertTarget("FeedbackRepository", feedbackRepository);
    assertTarget("FriendRepository", friendRepository);
    assertTarget("GuestbookRepository", guestbookRepository);
    assertTarget("PasswordResetTokenRepository", passwordResetTokenRepository);
    assertTarget("UserRepository", userRepository);
    assertTarget("MailOutboxRepository", mailOutboxRepository);
    assertTarget("FileRepository", fileRepository);
    assertTarget("MomentRepository", momentRepository);
    assertTarget("NotificationRepository", notificationRepository);
    assertTarget("SystemDatabaseInfoRepository", systemDatabaseInfoRepository);
    assertTarget("RssFeedRepository", rssFeedRepository);
    assertTarget("MenuRepository", menuRepository);
    assertTarget("SettingRepository", settingRepository);
    assertTarget("SubscriberRepository", subscriberRepository);
    assertTarget("TaxonomyRepository", taxonomyRepository);
  }

  @Test
  void replacedJdbcRepositoriesAreDeleted() {
    assertThat(REPLACED_JDBC_CLASSES).noneSatisfy(className -> assertThat(classExists(className)).isTrue());
  }

  @Test
  void notificationTypeTextKeepsExistingChineseLabels() {
    long id =
        notificationRepository.create(
            "article_published",
            "Published",
            "Article published",
            "/articles/demo",
            Map.of("source", "batch22"),
            1001L,
            "system");

    assertThat(notificationRepository.get(id).get("type_text")).isEqualTo("文章发布");
  }

  private static void assertTarget(String portName, Object bean) {
    assertThat(AopUtils.getTargetClass(bean).getName()).isEqualTo(EXPECTED_MYBATIS_TARGETS.get(portName));
  }

  private static boolean classExists(String className) {
    try {
      Class.forName(className);
      return true;
    } catch (ClassNotFoundException exception) {
      return false;
    }
  }
}
