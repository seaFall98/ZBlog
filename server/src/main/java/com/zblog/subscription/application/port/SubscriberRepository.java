package com.zblog.subscription.application.port;

import com.zblog.common.api.PageResponse;
import java.util.List;
import java.util.Map;

public interface SubscriberRepository {

  List<Long> findIdsByEmail(String email);

  long create(String email, String unsubscribeToken);

  void reactivate(long id);

  List<Long> findActiveIdsByToken(String token);

  void deactivate(long id);

  PageResponse<Map<String, Object>> listAdmin(int page, int pageSize);

  void delete(long id);

  Map<String, Object> get(long id);
}
