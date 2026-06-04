package com.zblog.feedback.application.port;

import java.util.Map;

public interface FeedbackMailer {

  void sendNewFeedback(String ownerEmail, String ticketNo, Map<String, Object> formContent);

  void sendReply(Map<String, Object> feedback, String recipient, String reply);
}
