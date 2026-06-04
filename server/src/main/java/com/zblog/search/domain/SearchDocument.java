package com.zblog.search.domain;

public record SearchDocument(
    long articleId,
    String title,
    String slug,
    String summary,
    String contentText,
    String publishedAt) {}
