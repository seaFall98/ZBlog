package com.zblog.media.infrastructure.mybatis;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileMapper {

  void insertFile(Map<String, Object> params);

  long countRows(
      @Param("keyword") String keyword,
      @Param("fileTypeLike") String fileTypeLike,
      @Param("status") Integer status,
      @Param("uploadType") String uploadType,
      @Param("minSize") Long minSize,
      @Param("maxSize") Long maxSize,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  List<Map<String, Object>> listRows(
      @Param("keyword") String keyword,
      @Param("fileTypeLike") String fileTypeLike,
      @Param("status") Integer status,
      @Param("uploadType") String uploadType,
      @Param("minSize") Long minSize,
      @Param("maxSize") Long maxSize,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("limit") int limit,
      @Param("offset") int offset);

  List<String> activeFilenames(@Param("id") long id);

  List<Map<String, Object>> activeStorageReferences(@Param("id") long id);

  void markDeleted(@Param("id") long id);
}
