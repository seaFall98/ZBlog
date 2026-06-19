package com.zblog.feedback.application.port;

import com.zblog.common.api.PageResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface FeedbackRepository {

  long create(
      String ticketNo,
      String accessToken,
      Long userId,
      String reportUrl,
      String reportType,
      String formContentJson,
      String email,
      String userAgent,
      String ip);

  Map<String, Object> getByTicket(String ticketNo);

  Map<String, Object> getByAccessToken(String accessToken);

  PageResponse<Map<String, Object>> listByUserId(long userId, Map<String, String> params);

  PageResponse<Map<String, Object>> listAdmin(Map<String, String> params);

  Map<String, Object> get(long id);

  void updateStatus(long id, String status);

  void updateAdminReply(long id, String reply);

  void touchUserReply(long id);

  void insertMessage(
      long feedbackId,
      String actorType,
      Long actorUserId,
      String messageType,
      String content,
      String attachmentsJson,
      String fromStatus,
      String toStatus);

  List<Map<String, Object>> listMessages(long feedbackId);

  void delete(long id);

  int deleteResolvedOrClosedOlderThan(LocalDateTime threshold);

  String ownerEmail();
}
