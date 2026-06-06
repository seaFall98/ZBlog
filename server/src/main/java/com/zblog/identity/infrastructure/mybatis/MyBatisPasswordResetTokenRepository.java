package com.zblog.identity.infrastructure.mybatis;

import com.zblog.identity.application.port.PasswordResetTokenRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisPasswordResetTokenRepository implements PasswordResetTokenRepository {

  private final PasswordResetTokenMapper passwordResetTokenMapper;

  public MyBatisPasswordResetTokenRepository(PasswordResetTokenMapper passwordResetTokenMapper) {
    this.passwordResetTokenMapper = passwordResetTokenMapper;
  }

  public void create(String email, String token, LocalDateTime expiresAt) {
    passwordResetTokenMapper.insertToken(email, token, expiresAt);
  }

  public Optional<Long> findNewestUsableTokenId(String email, String token) {
    List<Long> ids = passwordResetTokenMapper.newestUsableTokenIds(email, token);
    return ids.isEmpty() ? Optional.empty() : Optional.of(ids.getFirst());
  }

  public void markUsed(long id) {
    passwordResetTokenMapper.markUsed(id);
  }
}
