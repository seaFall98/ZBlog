package com.zblog.guestbook.domain;

import java.util.Locale;

public enum GuestbookStatus {
  PENDING("pending"),
  APPROVED("approved"),
  REJECTED("rejected"),
  HIDDEN("hidden");

  private final String value;

  GuestbookStatus(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }

  public static GuestbookStatus from(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("status is required");
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    for (GuestbookStatus status : values()) {
      if (status.value.equals(normalized)) {
        return status;
      }
    }
    throw new IllegalArgumentException("invalid guestbook status");
  }
}
