package com.zblog.scheduler;

import java.time.LocalDateTime;

public record ScheduledJobLog(
    long id,
    long jobId,
    String jobName,
    String handlerName,
    String status,
    String message,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    Long durationMs,
    LocalDateTime createdAt) {}
