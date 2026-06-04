package com.zblog.stats.domain;

import java.time.LocalDateTime;

public record VisitEventInput(
    String visitorId,
    String eventType,
    String url,
    String hostname,
    String title,
    String referrer,
    String language,
    String screen,
    Long articleId,
    String eventName,
    String eventData,
    Long durationSeconds,
    String ip,
    String userAgent,
    LocalDateTime createdAt) {}
