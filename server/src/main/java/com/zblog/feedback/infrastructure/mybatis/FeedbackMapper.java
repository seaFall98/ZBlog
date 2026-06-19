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

  List<Map<String, Object>> rowsByAccessToken(@Param("accessToken") String accessToken);

  long countByUserId(@Param("userId") long userId, @Param("status") String status);

  List<Map<String, Object>> listByUserId(
      @Param("userId") long userId,
      @Param("status") String status,
      @Param("limit") int limit,
      @Param("offset") int offset);

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

  void updateStatus(@Param("id") long id, @Param("status") String status);

  void updateAdminReply(@Param("id") long id, @Param("reply") String reply);

  void touchUserReply(@Param("id") long id);

  void insertMessage(Map<String, Object> params);

  List<Map<String, Object>> listMessages(@Param("feedbackId") long feedbackId);

  void delete(@Param("id") long id);

  int deleteResolvedOrClosedOlderThan(@Param("threshold") LocalDateTime threshold);

  List<String> ownerEmailValues();
}
