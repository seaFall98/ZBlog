package com.zblog.mail;

import org.springframework.stereotype.Component;

@Component
public class OutboxMailSender implements MailSender {

  // 当前 sender 只表达 outbox 投递边界；真正 SMTP worker 属于后续运维能力。
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
