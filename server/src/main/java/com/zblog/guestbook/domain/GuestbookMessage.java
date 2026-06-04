package com.zblog.guestbook.domain;

import java.time.LocalDateTime;

public record GuestbookMessage(
    long id,
    String nickname,
    String email,
    String content,
    GuestbookStatus status,
    boolean pinned,
    String ip,
    String userAgent,
    String adminNote,
    boolean deleted,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
