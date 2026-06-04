package com.zblog.identity.application.port;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository {

  void create(String email, String token, LocalDateTime expiresAt);

  Optional<Long> findNewestUsableTokenId(String email, String token);

  void markUsed(long id);
}
