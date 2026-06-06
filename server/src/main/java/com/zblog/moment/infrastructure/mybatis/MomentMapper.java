package com.zblog.moment.infrastructure.mybatis;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MomentMapper {

  List<Map<String, Object>> findAll();

  List<Map<String, Object>> rowsById(@Param("id") long id);

  void insertMoment(Map<String, Object> params);

  void updateMoment(
      @Param("id") long id,
      @Param("contentJson") String contentJson,
      @Param("publish") boolean publish,
      @Param("publishTime") Instant publishTime);

  void deleteMoment(@Param("id") long id);
}
