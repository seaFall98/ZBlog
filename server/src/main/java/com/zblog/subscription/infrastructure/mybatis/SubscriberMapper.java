package com.zblog.subscription.infrastructure.mybatis;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SubscriberMapper {

  List<Long> idsByEmail(@Param("email") String email);

  void insertSubscriber(Map<String, Object> params);

  void reactivate(@Param("id") long id);

  List<Long> activeIdsByToken(@Param("token") String token);

  void deactivate(@Param("id") long id);

  long countActiveRows();

  List<Map<String, Object>> listAdminRows(@Param("limit") int limit, @Param("offset") int offset);

  void delete(@Param("id") long id);

  List<Map<String, Object>> rowsById(@Param("id") long id);
}
