package com.zblog.subscription.application;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.subscription.application.port.SubscriberRepository;
import com.zblog.subscription.application.port.SubscriptionMailer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

  private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
  private static final int DELIVERY_BATCH_SIZE = 500;

  private final SubscriberRepository subscriberRepository;
  private final SubscriptionMailer subscriptionMailer;

  public SubscriptionService(SubscriberRepository subscriberRepository, SubscriptionMailer subscriptionMailer) {
    this.subscriberRepository = subscriberRepository;
    this.subscriptionMailer = subscriptionMailer;
  }

  @Transactional
  public Map<String, Object> subscribe(Map<String, Object> request) {
    String email = text(request, "email").trim().toLowerCase();
    if (!EMAIL_PATTERN.matcher(email).matches()) {
      throw new BusinessException(40001, "Invalid email", HttpStatus.BAD_REQUEST);
    }

    String unsubscribeToken = token();
    String confirmationToken = token();
    List<Long> existing = subscriberRepository.findIdsByEmail(email);
    long id;
    if (existing.isEmpty()) {
      id = subscriberRepository.create(email, unsubscribeToken, confirmationToken);
    } else {
      id = existing.getFirst();
      subscriberRepository.resetPending(id, unsubscribeToken, confirmationToken);
    }
    Map<String, Object> subscriber = subscriberRepository.get(id);
    subscriptionMailer.sendSubscribeConfirm(subscriber);
    return subscriber;
  }

  @Transactional
  public Map<String, Object> confirm(String token) {
    List<Long> ids = subscriberRepository.findIdsByConfirmationToken(token);
    if (ids.isEmpty()) {
      throw new BusinessException(404, "Subscriber not found", HttpStatus.NOT_FOUND);
    }
    long id = ids.getFirst();
    subscriberRepository.activate(id);
    return subscriberRepository.get(id);
  }

  @Transactional
  public Map<String, Object> unsubscribe(String token) {
    List<Long> ids = subscriberRepository.findActiveIdsByToken(token);
    if (ids.isEmpty()) {
      throw new BusinessException(404, "Subscriber not found", HttpStatus.NOT_FOUND);
    }
    long id = ids.getFirst();
    subscriberRepository.deactivate(id);
    Map<String, Object> subscriber = subscriberRepository.get(id);
    subscriptionMailer.sendUnsubscribeConfirm(subscriber);
    return subscriber;
  }

  public PageResponse<Map<String, Object>> listAdmin(int page, int pageSize) {
    return subscriberRepository.listAdmin(page, pageSize);
  }

  public PageResponse<Map<String, Object>> listAdmin(int page, int pageSize, String keyword, String status) {
    return subscriberRepository.listAdmin(page, pageSize, keyword, status);
  }

  public void delete(long id) {
    subscriberRepository.delete(id);
  }

  @Transactional
  public Map<String, Object> enqueueArticlePublished(Map<String, Object> article) {
    int offset = 0;
    int total = 0;
    int queued = 0;
    int failed = 0;
    while (true) {
      List<Map<String, Object>> subscribers = subscriberRepository.listActiveSubscribers(DELIVERY_BATCH_SIZE, offset);
      if (subscribers.isEmpty()) {
        break;
      }
      for (Map<String, Object> subscriber : subscribers) {
        total++;
        try {
          subscriptionMailer.sendArticlePublished(subscriber, article);
          subscriberRepository.recordDeliveryQueued(((Number) subscriber.get("id")).longValue());
          queued++;
        } catch (RuntimeException exception) {
          subscriberRepository.markBounced(((Number) subscriber.get("id")).longValue(), exception.getMessage());
          failed++;
        }
      }
      offset += subscribers.size();
      if (subscribers.size() < DELIVERY_BATCH_SIZE) {
        break;
      }
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("total", total);
    result.put("queued", queued);
    result.put("failed", failed);
    return result;
  }

  private String text(Map<String, Object> request, String key) {
    Object value = request.get(key);
    return value == null ? "" : value.toString();
  }

  private String token() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
