package com.zblog.feedback.application.port;

import java.util.Map;

public interface FeedbackNotifier {

  void notifyNewFeedback(Map<String, Object> feedback);
}
