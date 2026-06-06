package com.zblog.identity.infrastructure.mybatis;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PasswordResetTokenMapper {

  void insertToken(
      @Param("email") String email,
      @Param("token") String token,
      @Param("expiresAt") LocalDateTime expiresAt);

  List<Long> newestUsableTokenIds(@Param("email") String email, @Param("token") String token);

  void markUsed(@Param("id") long id);
}
