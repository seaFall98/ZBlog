package com.zblog.rssfeed.application;

import java.time.LocalDateTime;

public record FeedItem(String title, String link, String description, LocalDateTime publishedAt) {}
