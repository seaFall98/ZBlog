package com.zblog.identity.domain;

import java.time.Instant;

public record UserAccount(
    long id,
    String email,
    String passwordHash,
    String nickname,
    String avatar,
    String badge,
    String website,
    String role,
    boolean enabled,
    Instant deletedAt,
    Instant lastLogin,
    String githubId,
    String googleId,
    String qqId,
    String microsoftId,
    String feishuOpenId,
    Instant createdAt,
    Instant updatedAt) {}
