package com.zblog2.site.domain;

public record Menu(
    long id, String type, Long parentId, String title, String url, String icon, int sort) {}
