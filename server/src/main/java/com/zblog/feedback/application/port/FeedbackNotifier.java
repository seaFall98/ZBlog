package com.zblog.feedback.application.port;

import java.util.Map;

public interface FeedbackNotifier {

  void notifyNewFeedback(Map<String, Object> feedback);

  void notifyFeedbackUserUpdate(long recipientUserId, Map<String, Object> feedback, String title, String content);
}
