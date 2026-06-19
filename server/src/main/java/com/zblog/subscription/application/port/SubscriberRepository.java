package com.zblog.subscription.application.port;

import com.zblog.common.api.PageResponse;
import java.util.List;
import java.util.Map;

public interface SubscriberRepository {

  List<Long> findIdsByEmail(String email);

  long create(String email, String unsubscribeToken);

  long create(String email, String unsubscribeToken, String confirmationToken);

  void resetPending(long id, String unsubscribeToken, String confirmationToken);

  void activate(long id);

  List<Long> findIdsByConfirmationToken(String token);

  void reactivate(long id);

  List<Long> findActiveIdsByToken(String token);

  void deactivate(long id);

  PageResponse<Map<String, Object>> listAdmin(int page, int pageSize);

  PageResponse<Map<String, Object>> listAdmin(int page, int pageSize, String keyword, String status);

  List<Map<String, Object>> listActiveSubscribers(int limit, int offset);

  void recordDeliveryQueued(long id);

  void markBounced(long id, String errorMessage);

  void delete(long id);

  Map<String, Object> get(long id);
}
