package com.zblog.subscription.domain;

public enum SubscriberStatus {
  PENDING("待确认", "warning"),
  ACTIVE("已确认", "success"),
  UNSUBSCRIBED("已退订", "info"),
  BOUNCED("暂停发送", "danger");

  private final String label;
  private final String tone;

  SubscriberStatus(String label, String tone) {
    this.label = label;
    this.tone = tone;
  }

  public String label() {
    return label;
  }

  public String tone() {
    return tone;
  }

  public static SubscriberStatus from(String value) {
    if (value == null || value.isBlank()) {
      return PENDING;
    }
    return valueOf(value.trim().toUpperCase());
  }
}
