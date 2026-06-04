package com.zblog.event.domain;

public record OutboxEvent(
    long id,
    String eventType,
    String aggregateType,
    long aggregateId,
    String payload,
    int attempts) {}
