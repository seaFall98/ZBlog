package com.zblog.feedback.domain;

import java.util.Set;

public enum FeedbackStatus {
  PENDING("待处理", "warning", false),
  IN_PROGRESS("处理中", "primary", false),
  WAITING_USER("待用户补充", "info", false),
  RESOLVED("已解决", "success", true),
  CLOSED("已关闭", "info", true);

  private final String label;
  private final String tone;
  private final boolean terminal;

  FeedbackStatus(String label, String tone, boolean terminal) {
    this.label = label;
    this.tone = tone;
    this.terminal = terminal;
  }

  public String label() {
    return label;
  }

  public String tone() {
    return tone;
  }

  public boolean terminal() {
    return terminal;
  }

  public boolean active() {
    return !terminal;
  }

  public static FeedbackStatus from(String value) {
    if (value == null || value.isBlank()) {
      return PENDING;
    }
    return valueOf(value.trim().toUpperCase());
  }

  public Set<FeedbackStatus> allowedNext() {
    return switch (this) {
      case PENDING -> Set.of(IN_PROGRESS, WAITING_USER, RESOLVED, CLOSED);
      case IN_PROGRESS -> Set.of(WAITING_USER, RESOLVED, CLOSED);
      case WAITING_USER -> Set.of(IN_PROGRESS, RESOLVED, CLOSED);
      case RESOLVED -> Set.of(CLOSED);
      case CLOSED -> Set.of();
    };
  }

  public boolean canTransitionTo(FeedbackStatus target) {
    return target == this || allowedNext().contains(target);
  }
}
