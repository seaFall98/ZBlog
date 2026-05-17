package com.zblog.stats.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.cache.BlogCache;
import com.zblog.cache.CacheProperties;
import com.zblog.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class VisitCollectionService {

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final BlogCache blogCache;
  private final CacheProperties cacheProperties;

  public VisitCollectionService(
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper,
      BlogCache blogCache,
      CacheProperties cacheProperties) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.blogCache = blogCache;
    this.cacheProperties = cacheProperties;
  }

  public Map<String, Object> collect(Map<String, Object> payload, HttpServletRequest request) {
    String type = text(payload.get("type"), "pageview");
    if (!type.equals("pageview") && !type.equals("duration") && !type.equals("event")) {
      throw new BusinessException(40060, "Unsupported collect type", HttpStatus.BAD_REQUEST);
    }
    String ip = clientIp(request);
    String userAgent = header(request, "User-Agent");
    String url = text(payload.get("url"), "");
    String visitorId = visitorId(ip, userAgent, text(payload.get("screen"), ""), text(payload.get("language"), ""));
    long currentRate =
        blogCache.incrementWithTtl(
            "zblog:collect:rate:" + visitorId,
            Duration.ofMinutes(1));
    if (currentRate > cacheProperties.getCollectRateLimitPerMinute()) {
      throw new BusinessException(42901, "Too many collect requests", HttpStatus.TOO_MANY_REQUESTS);
    }
    LocalDateTime createdAt = timestamp(payload.get("timestamp"));
    Long articleId = number(payload.get("article_id"));
    jdbcTemplate.update(
        """
        insert into visit_events (
          visitor_id, event_type, url, hostname, title, referrer, language, screen,
          article_id, event_name, event_data, duration_seconds, ip, user_agent, created_at
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        visitorId,
        type,
        url,
        text(payload.get("hostname"), ""),
        text(payload.get("title"), ""),
        text(payload.get("referrer"), ""),
        text(payload.get("language"), ""),
        text(payload.get("screen"), ""),
        articleId,
        text(payload.get("event_name"), ""),
        writeJson(payload.get("event_data")),
        number(payload.get("duration")),
        ip,
        userAgent,
        createdAt);
    blogCache.delete("zblog:stats:site");
    boolean articleViewCounted = false;
    Long articleViewCount = null;
    if (type.equals("pageview") && articleId != null) {
      String dedupKey = "zblog:article:view:dedup:v2:" + articleId + ":" + visitorId;
      articleViewCounted =
          blogCache.setIfAbsent(
              dedupKey,
              "1",
              Duration.ofSeconds(cacheProperties.getArticleViewDedupSeconds()));
      if (articleViewCounted) {
        jdbcTemplate.update(
            "update articles set view_count = view_count + 1, updated_at = updated_at where id = ? and status = 'PUBLISHED'",
            articleId);
        blogCache.incrementHotArticle(articleId, 1);
      }
      articleViewCount = articleViewCount(articleId);
    }
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("accepted", true);
    response.put("visitor_id", visitorId);
    if (type.equals("pageview") && articleId != null) {
      response.put("article_view_counted", articleViewCounted);
      response.put("article_view_count", articleViewCount);
    }
    return response;
  }

  private Long articleViewCount(long articleId) {
    return jdbcTemplate.queryForObject("select view_count from articles where id = ?", Long.class, articleId);
  }

  private String visitorId(String ip, String userAgent, String screen, String language) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest((ip + "|" + userAgent + "|" + screen + "|" + language).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash).substring(0, 32);
    } catch (Exception exception) {
      throw new BusinessException(500, "Unable to create visitor id", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private LocalDateTime timestamp(Object value) {
    if (value instanceof Number number) {
      return LocalDateTime.ofInstant(Instant.ofEpochMilli(number.longValue()), ZoneId.systemDefault());
    }
    return LocalDateTime.now();
  }

  private Long number(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof String string && !string.isBlank()) {
      return Long.parseLong(string);
    }
    return null;
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value == null ? Map.of() : value);
    } catch (JsonProcessingException exception) {
      throw new BusinessException(40061, "Invalid event data", HttpStatus.BAD_REQUEST);
    }
  }

  private String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr() == null ? "" : request.getRemoteAddr();
  }

  private String header(HttpServletRequest request, String name) {
    String value = request.getHeader(name);
    return value == null ? "" : value;
  }

  private String text(Object value, String fallback) {
    return value == null ? fallback : value.toString();
  }
}
