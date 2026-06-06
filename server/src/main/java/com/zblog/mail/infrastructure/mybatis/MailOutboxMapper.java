package com.zblog.mail.infrastructure.mybatis;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MailOutboxMapper {

  void insertMail(Map<String, Object> params);

  void markSent(@Param("id") long id);

  void markFailed(@Param("id") long id, @Param("errorMessage") String errorMessage);

  long countRows(@Param("status") String status);

  List<Map<String, Object>> listRows(
      @Param("status") String status, @Param("limit") int limit, @Param("offset") int offset);

  List<Map<String, Object>> pendingForDelivery(@Param("limit") int limit);

  void markAttempting(@Param("id") long id);
}
