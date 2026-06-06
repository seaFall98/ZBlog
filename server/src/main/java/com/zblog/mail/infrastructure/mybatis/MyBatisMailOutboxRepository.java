package com.zblog.mail.infrastructure.mybatis;

import com.zblog.common.api.PageResponse;
import com.zblog.mail.application.port.MailOutboxRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisMailOutboxRepository implements MailOutboxRepository {

  private final MailOutboxMapper mailOutboxMapper;

  public MyBatisMailOutboxRepository(MailOutboxMapper mailOutboxMapper) {
    this.mailOutboxMapper = mailOutboxMapper;
  }

  public long create(String audience, String type, String recipient, String subject, String body) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("audience", audience);
    params.put("type", type);
    params.put("recipient", recipient);
    params.put("subject", subject);
    params.put("body", body);
    mailOutboxMapper.insertMail(params);
    return ((Number) params.get("id")).longValue();
  }

  public void markSent(long id) {
    mailOutboxMapper.markSent(id);
  }

  public void markFailed(long id, String errorMessage) {
    mailOutboxMapper.markFailed(id, errorMessage);
  }

  public PageResponse<Map<String, Object>> list(int page, int pageSize, String status) {
    int offset = Math.max(0, page - 1) * pageSize;
    String normalizedStatus = blankToNull(status);
    List<Map<String, Object>> rows = mailOutboxMapper.listRows(normalizedStatus, pageSize, offset);
    rows.forEach(this::formatTimestamps);
    return new PageResponse<>(rows, mailOutboxMapper.countRows(normalizedStatus), page, pageSize);
  }

  public List<Map<String, Object>> pendingForDelivery(int limit) {
    return mailOutboxMapper.pendingForDelivery(limit);
  }

  public void markAttempting(long id) {
    mailOutboxMapper.markAttempting(id);
  }

  private void formatTimestamps(Map<String, Object> row) {
    formatTimestamp(row, "last_attempt_at");
    formatTimestamp(row, "next_attempt_at");
    formatTimestamp(row, "created_at");
    formatTimestamp(row, "sent_at");
    formatTimestamp(row, "updated_at");
  }

  private void formatTimestamp(Map<String, Object> row, String key) {
    Object value = row.get(key);
    if (value != null) {
      row.put(key, value.toString());
    }
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
