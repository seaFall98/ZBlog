package com.zblog.identity.infrastructure.mybatis;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

  List<Map<String, Object>> rowsByEmail(@Param("email") String email);

  List<Map<String, Object>> rowsById(@Param("id") long id);

  int countByEmail(@Param("email") String email);

  void insertUser(Map<String, Object> params);

  void updateLastLogin(@Param("id") long id);

  void updateProfile(
      @Param("id") long id,
      @Param("email") String email,
      @Param("nickname") String nickname,
      @Param("avatar") String avatar,
      @Param("badge") String badge,
      @Param("website") String website);

  void updatePassword(@Param("id") long id, @Param("passwordHash") String passwordHash);

  void softDelete(@Param("id") long id);

  void clearOauthProvider(@Param("id") long id, @Param("column") String column);

  long countAdminRows(
      @Param("deleted") boolean deleted,
      @Param("keyword") String keyword,
      @Param("role") String role,
      @Param("enabled") Boolean enabled,
      @Param("loginMethod") String loginMethod);

  List<Map<String, Object>> listAdminRows(
      @Param("deleted") boolean deleted,
      @Param("keyword") String keyword,
      @Param("role") String role,
      @Param("enabled") Boolean enabled,
      @Param("loginMethod") String loginMethod,
      @Param("limit") int limit,
      @Param("offset") int offset);

  void updateAdmin(Map<String, Object> params);
}
