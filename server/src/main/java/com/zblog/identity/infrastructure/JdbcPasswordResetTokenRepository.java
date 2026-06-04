package com.zblog.identity.infrastructure;

import com.zblog.identity.application.port.PasswordResetTokenRepository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPasswordResetTokenRepository implements PasswordResetTokenRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcPasswordResetTokenRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void create(String email, String token, LocalDateTime expiresAt) {
    jdbcTemplate.update(
        "insert into password_reset_tokens (email, token, expires_at) values (?, ?, ?)",
        email,
        token,
        Timestamp.valueOf(expiresAt));
  }

  public Optional<Long> findNewestUsableTokenId(String email, String token) {
    List<Long> ids =
        jdbcTemplate.query(
            """
            select id from password_reset_tokens
            where email = ? and token = ? and used_at is null and expires_at > current_timestamp
            order by id desc
            """,
            (rs, rowNum) -> rs.getLong("id"),
            email,
            token);
    return ids.isEmpty() ? Optional.empty() : Optional.of(ids.getFirst());
  }

  public void markUsed(long id) {
    jdbcTemplate.update("update password_reset_tokens set used_at = current_timestamp where id = ?", id);
  }
}
