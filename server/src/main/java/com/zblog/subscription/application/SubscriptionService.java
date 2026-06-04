package com.zblog.subscription.application;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.subscription.application.port.SubscriberRepository;
import com.zblog.subscription.application.port.SubscriptionMailer;
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
    List<Long> existing = subscriberRepository.findIdsByEmail(email);
    if (existing.isEmpty()) {
      long id = subscriberRepository.create(email, UUID.randomUUID().toString().replace("-", ""));
      Map<String, Object> subscriber = subscriberRepository.get(id);
      subscriptionMailer.sendSubscribeConfirm(subscriber);
      return subscriber;
    }
    long id = existing.getFirst();
    subscriberRepository.reactivate(id);
    Map<String, Object> subscriber = subscriberRepository.get(id);
    subscriptionMailer.sendSubscribeConfirm(subscriber);
    return subscriber;
  }

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

  public void delete(long id) {
    subscriberRepository.delete(id);
  }

  private String text(Map<String, Object> request, String key) {
    Object value = request.get(key);
    return value == null ? "" : value.toString();
  }
}
