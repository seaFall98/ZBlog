package com.zblog.event.application.port;

import com.zblog.common.api.PageResponse;
import com.zblog.event.domain.OutboxEvent;
import java.util.List;
import java.util.Map;

public interface EventOutboxRepository {

  void createArticleEvent(String eventType, long aggregateId, String payload);

  PageResponse<Map<String, Object>> list(int page, int pageSize, String status);

  List<OutboxEvent> pendingForPublish();

  void markProcessing(long eventId);

  void markSent(long eventId);

  void markFailed(long eventId, String errorMessage);
}
