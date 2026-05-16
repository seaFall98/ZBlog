package com.zblog.mail;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MailOutboxService {

  private final JdbcTemplate jdbcTemplate;
  private final MailSender mailSender;

  public MailOutboxService(JdbcTemplate jdbcTemplate, MailSender mailSender) {
    this.jdbcTemplate = jdbcTemplate;
    this.mailSender = mailSender;
  }

  @Transactional
  public void send(String audience, String type, String recipient, String subject, String body) {
    long id = insert(audience, type, recipient, subject, body);
    try {
      mailSender.send(recipient, subject, body);
      jdbcTemplate.update(
          "update mail_outbox set status = 'sent', sent_at = current_timestamp where id = ?", id);
    } catch (RuntimeException exception) {
      jdbcTemplate.update(
          "update mail_outbox set status = 'failed', error_message = ? where id = ?",
          exception.getMessage(),
          id);
      throw exception;
    }
  }

  private long insert(String audience, String type, String recipient, String subject, String body) {
    org.springframework.jdbc.support.KeyHolder keyHolder =
        new org.springframework.jdbc.support.GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var statement =
              connection.prepareStatement(
                  """
                  insert into mail_outbox (audience, mail_type, recipient, subject, body, status)
                  values (?, ?, ?, ?, ?, 'pending')
                  """,
                  java.sql.Statement.RETURN_GENERATED_KEYS);
          statement.setString(1, audience);
          statement.setString(2, type);
          statement.setString(3, recipient);
          statement.setString(4, subject);
          statement.setString(5, body);
          return statement;
        },
        keyHolder);
    Map<String, Object> keys = keyHolder.getKeys();
    if (keys != null && keys.get("id") instanceof Number number) {
      return number.longValue();
    }
    return keyHolder.getKey().longValue();
  }
}
