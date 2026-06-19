package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.zblog.common.api.PageResponse;
import com.zblog.subscription.application.SubscriptionService;
import com.zblog.subscription.application.port.SubscriberRepository;
import com.zblog.subscription.application.port.SubscriptionMailer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class P4SubscriptionServiceTest {

  @Test
  void articlePublishedDeliveryDoesNotSkipSubscribersAfterBounce() {
    FakeSubscriberRepository subscriberRepository = new FakeSubscriberRepository();
    for (long id = 1; id <= 501; id++) {
      subscriberRepository.addActive(id, "p4-batch-" + id + "@example.com");
    }
    RecordingMailer mailer = new RecordingMailer(1);

    Map<String, Object> result =
        new SubscriptionService(subscriberRepository, mailer)
            .enqueueArticlePublished(Map.of("title", "P4 delivery", "slug", "p4-delivery"));

    assertThat(result).containsEntry("total", 501).containsEntry("queued", 500).containsEntry("failed", 1);
    assertThat(mailer.attemptedIds).hasSize(501);
    assertThat(mailer.attemptedIds).contains(501L);
    assertThat(subscriberRepository.queuedIds).hasSize(500).contains(501L);
    assertThat(subscriberRepository.bouncedIds).containsExactly(1L);
  }

  private static final class RecordingMailer implements SubscriptionMailer {
    private final long failingId;
    private final List<Long> attemptedIds = new ArrayList<>();

    private RecordingMailer(long failingId) {
      this.failingId = failingId;
    }

    public void sendSubscribeConfirm(Map<String, Object> subscriber) {}

    public void sendUnsubscribeConfirm(Map<String, Object> subscriber) {}

    public void sendArticlePublished(Map<String, Object> subscriber, Map<String, Object> article) {
      long id = ((Number) subscriber.get("id")).longValue();
      attemptedIds.add(id);
      if (id == failingId) {
        throw new IllegalStateException("mail failed");
      }
    }
  }

  private static final class FakeSubscriberRepository implements SubscriberRepository {
    private final Map<Long, Map<String, Object>> subscribers = new LinkedHashMap<>();
    private final List<Long> queuedIds = new ArrayList<>();
    private final List<Long> bouncedIds = new ArrayList<>();

    private void addActive(long id, String email) {
      Map<String, Object> subscriber = new LinkedHashMap<>();
      subscriber.put("id", id);
      subscriber.put("email", email);
      subscriber.put("status", "ACTIVE");
      subscribers.put(id, subscriber);
    }

    public List<Long> findIdsByEmail(String email) {
      throw unsupported();
    }

    public long create(String email, String unsubscribeToken) {
      throw unsupported();
    }

    public long create(String email, String unsubscribeToken, String confirmationToken) {
      throw unsupported();
    }

    public void resetPending(long id, String unsubscribeToken, String confirmationToken) {
      throw unsupported();
    }

    public void activate(long id) {
      throw unsupported();
    }

    public List<Long> findIdsByConfirmationToken(String token) {
      throw unsupported();
    }

    public void reactivate(long id) {
      throw unsupported();
    }

    public List<Long> findActiveIdsByToken(String token) {
      throw unsupported();
    }

    public void deactivate(long id) {
      throw unsupported();
    }

    public PageResponse<Map<String, Object>> listAdmin(int page, int pageSize) {
      throw unsupported();
    }

    public PageResponse<Map<String, Object>> listAdmin(int page, int pageSize, String keyword, String status) {
      throw unsupported();
    }

    public List<Map<String, Object>> listActiveSubscribersAfterId(int limit, long afterId) {
      return subscribers.values().stream()
          .filter(row -> "ACTIVE".equals(row.get("status")))
          .filter(row -> ((Number) row.get("id")).longValue() > afterId)
          .limit(limit)
          .map(LinkedHashMap::new)
          .map(row -> (Map<String, Object>) row)
          .toList();
    }

    public void recordDeliveryQueued(long id) {
      queuedIds.add(id);
    }

    public void markBounced(long id, String errorMessage) {
      subscribers.get(id).put("status", "BOUNCED");
      bouncedIds.add(id);
    }

    public void delete(long id) {
      throw unsupported();
    }

    public Map<String, Object> get(long id) {
      throw unsupported();
    }

    private UnsupportedOperationException unsupported() {
      return new UnsupportedOperationException("not needed by this test");
    }
  }
}
