package com.zblog.feedback.domain;

public enum FeedbackActorType {
  USER,
  ADMIN,
  SYSTEM;

  public static FeedbackActorType from(String value) {
    if (value == null || value.isBlank()) {
      return SYSTEM;
    }
    return valueOf(value.trim().toUpperCase());
  }
}

