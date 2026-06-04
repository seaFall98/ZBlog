package com.zblog.identity.infrastructure;

import com.zblog.identity.application.port.PasswordResetMailer;
import com.zblog.mail.MailOutboxService;
import org.springframework.stereotype.Component;

@Component
public class OutboxPasswordResetMailer implements PasswordResetMailer {

  private final MailOutboxService mailOutboxService;

  public OutboxPasswordResetMailer(MailOutboxService mailOutboxService) {
    this.mailOutboxService = mailOutboxService;
  }

  public void sendResetToken(String email, String token) {
    mailOutboxService.send("user", "password_reset", email, "重置密码", "重置密码 token: " + token);
  }
}
