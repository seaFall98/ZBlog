package com.zblog.content.domain;

public enum ArticleCopyrightType {
  ORIGINAL("原创"),
  REPOST("转载"),
  TRANSLATION("翻译");

  private final String label;

  ArticleCopyrightType(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public static ArticleCopyrightType from(String value) {
    if (value == null || value.isBlank()) {
      return ORIGINAL;
    }
    return valueOf(value.trim().toUpperCase());
  }
}

