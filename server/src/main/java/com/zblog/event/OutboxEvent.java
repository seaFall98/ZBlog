package com.zblog.event;

public record OutboxEvent(
    long id,
    String eventType,
    String aggregateType,
    long aggregateId,
    String payload,
    int attempts) {}
