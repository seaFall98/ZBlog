package com.zblog.stats.application;

public record VisitQuery(
    int page,
    int pageSize,
    String keyword,
    String visitorId,
    String ip,
    String excludeIps,
    String location,
    String browser,
    String os,
    String startTime,
    String endTime) {}
