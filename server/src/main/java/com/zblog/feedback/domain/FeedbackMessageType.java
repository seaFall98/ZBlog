package com.zblog.feedback.domain;

public enum FeedbackMessageType {
  MESSAGE,
  STATUS_CHANGE;

  public static FeedbackMessageType from(String value) {
    if (value == null || value.isBlank()) {
      return MESSAGE;
    }
    return valueOf(value.trim().toUpperCase());
  }
}

