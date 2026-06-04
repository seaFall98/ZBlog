package com.zblog.content.domain;

public record ArticleSearchProjection(
    long articleId, String title, String slug, String summary, String contentText, String publishedAt) {}
