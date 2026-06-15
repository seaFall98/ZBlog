package com.zblog.event.infrastructure.mybatis;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EventOutboxMapper {

  void insertArticleEvent(
      @Param("eventType") String eventType,
      @Param("aggregateId") long aggregateId,
      @Param("payload") String payload);

  void insertEvent(
      @Param("eventType") String eventType,
      @Param("aggregateType") String aggregateType,
      @Param("aggregateId") long aggregateId,
      @Param("payload") String payload);

  long countRows(@Param("status") String status);

  List<Map<String, Object>> listRows(
      @Param("status") String status, @Param("limit") int limit, @Param("offset") int offset);

  List<Map<String, Object>> pendingForPublish(@Param("staleProcessingBefore") LocalDateTime staleProcessingBefore);

  void markProcessing(@Param("eventId") long eventId);

  void markSent(@Param("eventId") long eventId);

  void markFailed(@Param("eventId") long eventId, @Param("errorMessage") String errorMessage);
}
