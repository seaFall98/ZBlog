package com.zblog.mail;

import org.springframework.stereotype.Component;

@Component
public class OutboxMailSender implements MailSender {

  private final MailProperties properties;

  public OutboxMailSender(MailProperties properties) {
    this.properties = properties;
  }

  @Override
  public void send(String recipient, String subject, String body) {
    if ("smtp".equalsIgnoreCase(properties.getMode()) && properties.getHost().isBlank()) {
      throw new IllegalStateException("SMTP host is not configured");
    }
  }
}
