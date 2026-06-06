package com.zblog.event.infrastructure.mybatis;

import com.zblog.common.api.PageResponse;
import com.zblog.event.application.port.EventOutboxRepository;
import com.zblog.event.domain.OutboxEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisEventOutboxRepository implements EventOutboxRepository {

  private final EventOutboxMapper eventOutboxMapper;

  public MyBatisEventOutboxRepository(EventOutboxMapper eventOutboxMapper) {
    this.eventOutboxMapper = eventOutboxMapper;
  }

  public void createArticleEvent(String eventType, long aggregateId, String payload) {
    eventOutboxMapper.insertArticleEvent(eventType, aggregateId, payload);
  }

  public PageResponse<Map<String, Object>> list(int page, int pageSize, String status) {
    int offset = Math.max(0, page - 1) * pageSize;
    String normalizedStatus = status == null || status.isBlank() ? null : status;
    List<Map<String, Object>> rows =
        eventOutboxMapper.listRows(normalizedStatus, pageSize, offset).stream().map(this::mapRow).toList();
    return new PageResponse<>(rows, eventOutboxMapper.countRows(normalizedStatus), page, pageSize);
  }

  public List<OutboxEvent> pendingForPublish() {
    return eventOutboxMapper.pendingForPublish().stream().map(this::toEvent).toList();
  }

  public void markProcessing(long eventId) {
    eventOutboxMapper.markProcessing(eventId);
  }

  public void markSent(long eventId) {
    eventOutboxMapper.markSent(eventId);
  }

  public void markFailed(long eventId, String errorMessage) {
    eventOutboxMapper.markFailed(eventId, errorMessage);
  }

  private OutboxEvent toEvent(Map<String, Object> row) {
    return new OutboxEvent(
        number(row.get("id")),
        string(row.get("event_type")),
        string(row.get("aggregate_type")),
        number(row.get("aggregate_id")),
        string(row.get("payload")),
        (int) number(row.get("attempts")));
  }

  private Map<String, Object> mapRow(Map<String, Object> source) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", source.get("id"));
    row.put("event_type", source.get("event_type"));
    row.put("aggregate_type", source.get("aggregate_type"));
    row.put("aggregate_id", source.get("aggregate_id"));
    row.put("payload", source.get("payload"));
    row.put("status", source.get("status"));
    row.put("attempts", source.get("attempts"));
    row.put("error_message", source.get("error_message"));
    row.put("sent_at", timestamp(source.get("sent_at")));
    row.put("created_at", timestamp(source.get("created_at")));
    row.put("updated_at", timestamp(source.get("updated_at")));
    return row;
  }

  private Object timestamp(Object value) {
    return value == null ? null : value.toString();
  }

  private long number(Object value) {
    return ((Number) value).longValue();
  }

  private String string(Object value) {
    return value == null ? "" : value.toString();
  }
}
