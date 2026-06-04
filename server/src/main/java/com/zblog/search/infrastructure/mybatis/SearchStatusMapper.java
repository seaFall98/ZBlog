package com.zblog.search.infrastructure.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SearchStatusMapper {

  int ensureRow(
      @Param("strategy") String strategy,
      @Param("elasticsearchEnabled") boolean elasticsearchEnabled,
      @Param("fallbackToDb") boolean fallbackToDb);

  SearchStatusRow status();

  int recordReindex(
      @Param("strategy") String strategy,
      @Param("elasticsearchEnabled") boolean elasticsearchEnabled,
      @Param("fallbackToDb") boolean fallbackToDb,
      @Param("indexed") int indexed,
      @Param("deleted") int deleted,
      @Param("failed") int failed);

  int recordError(
      @Param("strategy") String strategy,
      @Param("elasticsearchEnabled") boolean elasticsearchEnabled,
      @Param("fallbackToDb") boolean fallbackToDb,
      @Param("message") String message);

  record SearchStatusRow(
      java.time.LocalDateTime lastReindexAt,
      int lastReindexIndexed,
      int lastReindexDeleted,
      int lastReindexFailed,
      String lastError,
      java.time.LocalDateTime lastErrorAt) {}
}
