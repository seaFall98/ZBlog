package com.zblog.mail.application.port;

import com.zblog.common.api.PageResponse;
import java.util.List;
import java.util.Map;

public interface MailOutboxRepository {

  long create(String audience, String type, String recipient, String subject, String body);

  void markSent(long id);

  void markFailed(long id, String errorMessage);

  PageResponse<Map<String, Object>> list(int page, int pageSize, String status);

  List<Map<String, Object>> pendingForDelivery(int limit);

  void markAttempting(long id);
}
