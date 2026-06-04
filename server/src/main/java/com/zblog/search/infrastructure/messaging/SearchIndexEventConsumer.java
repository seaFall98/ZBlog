package com.zblog.search.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.exception.BusinessException;
import com.zblog.event.application.port.EventConsumptionRepository;
import com.zblog.search.application.SearchService;
import com.zblog.search.application.SearchStatusService;
import com.zblog.search.domain.SearchDocument;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchIndexEventConsumer {

  private static final String CONSUMER_NAME = "article-search-index";

  private final EventConsumptionRepository eventConsumptionRepository;
  private final ObjectMapper objectMapper;
  private final SearchService searchService;
  private final SearchStatusService statusService;

  public SearchIndexEventConsumer(
      EventConsumptionRepository eventConsumptionRepository,
      ObjectMapper objectMapper,
      SearchService searchService,
      SearchStatusService statusService) {
    this.eventConsumptionRepository = eventConsumptionRepository;
    this.objectMapper = objectMapper;
    this.searchService = searchService;
    this.statusService = statusService;
  }

  @Transactional
  public void consume(long eventId, String eventType, String payload) {
    // 重复投递直接跳过，避免同一 outbox 事件重复写入索引。
    if (eventConsumptionRepository.alreadyConsumed(eventId, CONSUMER_NAME)) {
      return;
    }
    try {
      Map<String, Object> data = readPayload(payload);
      if ("ARTICLE_SEARCH_UPSERT".equals(eventType)) {
        searchService.upsert(toDocument(data));
      } else if ("ARTICLE_SEARCH_DELETE".equals(eventType)) {
        searchService.delete(articleId(data));
      } else {
        return;
      }
      eventConsumptionRepository.markConsumed(eventId, CONSUMER_NAME);
    } catch (RuntimeException exception) {
      // 索引失败要记录运维可见状态，并继续抛出以保留 outbox 重试语义。
      statusService.recordError(exception.getMessage());
      throw exception;
    }
  }


  private SearchDocument toDocument(Map<String, Object> data) {
    return new SearchDocument(
        articleId(data),
        string(data, "title"),
        string(data, "slug"),
        string(data, "summary"),
        string(data, "content_text"),
        string(data, "published_at"));
  }

  private long articleId(Map<String, Object> data) {
    Object value = data.get("article_id");
    if (value instanceof Number number) {
      return number.longValue();
    }
    throw new BusinessException(500, "Invalid search event payload: missing article_id", HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private String string(Map<String, Object> data, String key) {
    Object value = data.get(key);
    return value == null ? "" : value.toString();
  }

  private Map<String, Object> readPayload(String payload) {
    try {
      return objectMapper.readValue(payload, new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      throw new BusinessException(500, "Invalid search event payload", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
