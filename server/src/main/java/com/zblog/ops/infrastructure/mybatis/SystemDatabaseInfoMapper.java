package com.zblog.ops.infrastructure.mybatis;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemDatabaseInfoMapper {

  String productName();

  long tableCount();

  int ping();
}
