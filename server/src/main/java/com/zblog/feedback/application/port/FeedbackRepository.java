package com.zblog.feedback.application.port;

import com.zblog.common.api.PageResponse;
import java.util.Map;

public interface FeedbackRepository {

  long create(
      String ticketNo,
      String reportUrl,
      String reportType,
      String formContentJson,
      String email,
      String userAgent,
      String ip);

  Map<String, Object> getByTicket(String ticketNo);

  PageResponse<Map<String, Object>> listAdmin(Map<String, String> params);

  Map<String, Object> get(long id);

  void update(long id, String status, String reply);

  void delete(long id);

  String ownerEmail();
}
