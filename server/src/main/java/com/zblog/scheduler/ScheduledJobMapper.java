package com.zblog.scheduler;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScheduledJobMapper {

  long countJobs();

  List<Map<String, Object>> listJobs(@Param("limit") int limit, @Param("offset") int offset);

  List<Map<String, Object>> rowsById(@Param("id") long id);

  int updateJob(Map<String, Object> params);

  int updateEnabled(@Param("id") long id, @Param("enabled") boolean enabled);

  int updateLastRunAt(@Param("id") long id);

  void insertLog(Map<String, Object> params);

  int finishLog(Map<String, Object> params);

  long countLogs(@Param("jobId") long jobId);

  List<Map<String, Object>> listLogs(@Param("jobId") long jobId, @Param("limit") int limit, @Param("offset") int offset);

  List<Map<String, Object>> logRowsById(@Param("id") long id);
}
