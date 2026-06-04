package com.zblog.mail;

import com.zblog.common.api.PageResponse;
import com.zblog.mail.application.port.MailOutboxRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MailOutboxService {

  // 邮件先进入 outbox 再尝试发送，失败状态保留给后续运维和重试能力。
  private final MailOutboxRepository mailOutboxRepository;
  private final MailSender mailSender;

  public MailOutboxService(MailOutboxRepository mailOutboxRepository, MailSender mailSender) {
    this.mailOutboxRepository = mailOutboxRepository;
    this.mailSender = mailSender;
  }

  @Transactional
  public void send(String audience, String type, String recipient, String subject, String body) {
    long id = mailOutboxRepository.create(audience, type, recipient, subject, body);
    try {
      deliver(id, recipient, subject, body);
    } catch (RuntimeException exception) {
      return;
    }
  }

  public PageResponse<Map<String, Object>> list(int page, int pageSize, String status) {
    return mailOutboxRepository.list(page, pageSize, status);
  }

  @Transactional
  public Map<String, Object> drainPending(int limit) {
    int total = 0;
    int sent = 0;
    int failed = 0;
    for (Map<String, Object> row : mailOutboxRepository.pendingForDelivery(Math.max(1, limit))) {
      total++;
      long id = ((Number) row.get("id")).longValue();
      try {
        deliver(id, row.get("recipient").toString(), row.get("subject").toString(), row.get("body").toString());
        sent++;
      } catch (RuntimeException exception) {
        failed++;
      }
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("total", total);
    result.put("sent", sent);
    result.put("failed", failed);
    return result;
  }

  private void deliver(long id, String recipient, String subject, String body) {
    try {
      mailOutboxRepository.markAttempting(id);
      mailSender.send(recipient, subject, body);
      mailOutboxRepository.markSent(id);
    } catch (RuntimeException exception) {
      mailOutboxRepository.markFailed(id, exception.getMessage());
      throw exception;
    }
  }
}
