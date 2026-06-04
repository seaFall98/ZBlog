package com.zblog.security;

import com.zblog.config.SecurityProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final SecurityProperties securityProperties;
  private final Clock clock;

  @Autowired
  public JwtService(SecurityProperties securityProperties) {
    this(securityProperties, Clock.systemUTC());
  }

  JwtService(SecurityProperties securityProperties, Clock clock) {
    this.securityProperties = securityProperties;
    this.clock = clock;
  }

  public String createAdminToken(String username) {
    Instant now = clock.instant();
    Instant expiresAt = now.plusSeconds(securityProperties.getTokenTtlMinutes() * 60);
    return Jwts.builder()
        .subject(username)
        .claim("role", "ADMIN")
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiresAt))
        .signWith(signingKey())
        .compact();
  }

  public Optional<String> parseUsername(String token) {
    try {
      return Optional.ofNullable(
          Jwts.parser()
              .verifyWith(signingKey())
              .build()
              .parseSignedClaims(token)
              .getPayload()
              .getSubject());
    } catch (JwtException | IllegalArgumentException exception) {
      // token 解析失败不暴露细节，由安全过滤链统一返回未认证结果。
      return Optional.empty();
    }
  }

  private SecretKey signingKey() {
    return Keys.hmacShaKeyFor(
        securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
  }
}
