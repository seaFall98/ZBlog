package com.zblog.site.infrastructure.mybatis;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MenuMapper {

  List<Map<String, Object>> findAllRows();

  List<Map<String, Object>> rowsById(@Param("id") long id);

  void insertMenu(Map<String, Object> params);

  void updateMenu(Map<String, Object> params);

  void deleteChildren(@Param("id") long id);

  void detachChildren(@Param("id") long id);

  void deleteMenu(@Param("id") long id);

  void deleteByTypes(@Param("types") List<String> types);
}
