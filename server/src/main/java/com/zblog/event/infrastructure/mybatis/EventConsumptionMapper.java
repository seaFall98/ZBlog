package com.zblog.event.infrastructure.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EventConsumptionMapper {

  long countConsumed(@Param("eventId") long eventId, @Param("consumerName") String consumerName);

  void insertConsumed(@Param("eventId") long eventId, @Param("consumerName") String consumerName);
}
