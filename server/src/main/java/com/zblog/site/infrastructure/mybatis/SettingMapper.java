package com.zblog.site.infrastructure.mybatis;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SettingMapper {

  List<Map<String, Object>> groupRows(@Param("group") String group);

  int updateValue(
      @Param("group") String group, @Param("key") String key, @Param("value") String value);

  void insertValue(
      @Param("group") String group, @Param("key") String key, @Param("value") String value);

  void deleteKey(@Param("group") String group, @Param("key") String key);

  void deleteGroup(@Param("group") String group);
}
