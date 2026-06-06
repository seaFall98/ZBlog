package com.zblog.feedback.infrastructure.mybatis;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FeedbackMapper {

  void insertFeedback(Map<String, Object> params);

  List<Map<String, Object>> rowsByTicket(@Param("ticketNo") String ticketNo);

  long countAdmin(
      @Param("keyword") String keyword,
      @Param("reportType") String reportType,
      @Param("status") String status,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  List<Map<String, Object>> listAdmin(
      @Param("keyword") String keyword,
      @Param("reportType") String reportType,
      @Param("status") String status,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("limit") int limit,
      @Param("offset") int offset);

  List<Map<String, Object>> rowsById(@Param("id") long id);

  void update(@Param("id") long id, @Param("status") String status, @Param("reply") String reply);

  void delete(@Param("id") long id);

  List<String> ownerEmailValues();
}
