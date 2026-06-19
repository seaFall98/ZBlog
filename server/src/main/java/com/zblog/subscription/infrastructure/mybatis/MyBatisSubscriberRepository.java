package com.zblog.subscription.infrastructure.mybatis;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.subscription.application.port.SubscriberRepository;
import com.zblog.subscription.domain.SubscriberStatus;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisSubscriberRepository implements SubscriberRepository {

  private final SubscriberMapper subscriberMapper;

  public MyBatisSubscriberRepository(SubscriberMapper subscriberMapper) {
    this.subscriberMapper = subscriberMapper;
  }

  public List<Long> findIdsByEmail(String email) {
    return subscriberMapper.idsByEmail(email);
  }

  public long create(String email, String unsubscribeToken) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("email", email);
    params.put("unsubscribeToken", unsubscribeToken);
    subscriberMapper.insertSubscriber(params);
    return ((Number) params.get("id")).longValue();
  }

  public long create(String email, String unsubscribeToken, String confirmationToken) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("email", email);
    params.put("unsubscribeToken", unsubscribeToken);
    params.put("confirmationToken", confirmationToken);
    subscriberMapper.insertSubscriberV2(params);
    return ((Number) params.get("id")).longValue();
  }

  public void resetPending(long id, String unsubscribeToken, String confirmationToken) {
    subscriberMapper.resetPending(id, unsubscribeToken, confirmationToken);
  }

  public void activate(long id) {
    subscriberMapper.activate(id);
  }

  public List<Long> findIdsByConfirmationToken(String token) {
    return subscriberMapper.idsByConfirmationToken(token);
  }

  public void reactivate(long id) {
    subscriberMapper.reactivate(id);
  }

  public List<Long> findActiveIdsByToken(String token) {
    return subscriberMapper.activeIdsByToken(token);
  }

  public void deactivate(long id) {
    subscriberMapper.deactivate(id);
  }

  public PageResponse<Map<String, Object>> listAdmin(int page, int pageSize) {
    int offset = Math.max(0, page - 1) * pageSize;
    List<Map<String, Object>> rows = subscriberMapper.listAdminRows(pageSize, offset);
    rows.forEach(this::normalizeRow);
    return new PageResponse<>(rows, subscriberMapper.countActiveRows(), page, pageSize);
  }

  public PageResponse<Map<String, Object>> listAdmin(int page, int pageSize, String keyword, String status) {
    int offset = Math.max(0, page - 1) * pageSize;
    String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.toLowerCase() + "%";
    String normalizedStatus = status == null || status.isBlank() ? null : SubscriberStatus.from(status).name();
    List<Map<String, Object>> rows = subscriberMapper.listAdminRowsV2(like, normalizedStatus, pageSize, offset);
    rows.forEach(this::normalizeRow);
    return new PageResponse<>(rows, subscriberMapper.countAdminRows(like, normalizedStatus), page, pageSize);
  }

  public List<Map<String, Object>> listActiveSubscribersAfterId(int limit, long afterId) {
    List<Map<String, Object>> rows = subscriberMapper.listActiveRowsAfterId(limit, afterId);
    rows.forEach(this::normalizeRow);
    return rows;
  }

  public void recordDeliveryQueued(long id) {
    subscriberMapper.recordDeliveryQueued(id);
  }

  public void markBounced(long id, String errorMessage) {
    subscriberMapper.markBounced(id, errorMessage);
  }

  public void delete(long id) {
    subscriberMapper.delete(id);
  }

  public Map<String, Object> get(long id) {
    Map<String, Object> row = subscriberMapper.rowsById(id).stream()
        .findFirst()
        .orElseThrow(() -> new BusinessException(404, "Subscriber not found", HttpStatus.NOT_FOUND));
    normalizeRow(row);
    return row;
  }

  private void normalizeRow(Map<String, Object> row) {
    SubscriberStatus status = SubscriberStatus.from(value(row, "status"));
    row.put("status", status.name());
    row.put("status_label", status.label());
    row.put("status_tone", status.tone());
    format(row, "confirmed_at");
    format(row, "unsubscribed_at");
    format(row, "bounced_at");
    format(row, "last_delivery_at");
    format(row, "created_at");
    format(row, "updated_at");
  }

  private String value(Map<String, Object> row, String key) {
    Object value = row.get(key);
    return value == null ? "" : value.toString();
  }

  private void format(Map<String, Object> row, String key) {
    Object value = row.get(key);
    if (value != null) {
      row.put(key, value.toString());
    }
  }
}
