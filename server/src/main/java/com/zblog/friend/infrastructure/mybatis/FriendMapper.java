package com.zblog.friend.infrastructure.mybatis;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FriendMapper {

  List<Map<String, Object>> listTypes(@Param("offset") int offset, @Param("limit") int limit);

  long countTypes();

  void insertType(Map<String, Object> params);

  List<Map<String, Object>> typeById(@Param("id") long id);

  void updateType(
      @Param("id") long id,
      @Param("name") String name,
      @Param("sort") int sort,
      @Param("visible") boolean visible);

  void deleteType(@Param("id") long id);

  List<Map<String, Object>> listAdmin(@Param("offset") int offset, @Param("limit") int limit);

  long countAdmin();

  List<Map<String, Object>> friendById(@Param("id") long id);

  void insertFriend(Map<String, Object> params);

  void updateFriend(Map<String, Object> params);

  void deleteFriend(@Param("id") long id);

  List<Map<String, Object>> groupedPublicRows();
}
