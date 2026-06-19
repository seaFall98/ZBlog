package com.zblog.feedback.domain;

public enum FeedbackType {
  COPYRIGHT,
  INAPPROPRIATE,
  SUMMARY,
  SUGGESTION;

  public static FeedbackType from(String value) {
    if (value == null || value.isBlank()) {
      return SUGGESTION;
    }
    return valueOf(value.trim().toUpperCase());
  }

  public String wireValue() {
    return name().toLowerCase();
  }
}

