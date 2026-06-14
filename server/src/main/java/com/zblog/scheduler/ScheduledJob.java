package com.zblog.scheduler;

import java.time.LocalDateTime;

public record ScheduledJob(
    long id,
    String name,
    String handlerName,
    String cronExpression,
    String parameters,
    boolean enabled,
    String description,
    LocalDateTime lastRunAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
