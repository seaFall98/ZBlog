package com.zblog.stats.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.exception.BusinessException;
import com.zblog.stats.application.port.StatsCache;
import com.zblog.stats.application.port.VisitRepository;
import com.zblog.stats.domain.VisitEventInput;
import com.zblog.stats.domain.VisitRequestContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class VisitCollectionService {

  // 访问采集在一个入口内完成匿名访客识别、限流、落库、去重和热度更新。
  private final VisitRepository visitRepository;
  private final StatsCache statsCache;
  private final ObjectMapper objectMapper;

  public VisitCollectionService(
      VisitRepository visitRepository, StatsCache statsCache, ObjectMapper objectMapper) {
    this.visitRepository = visitRepository;
    this.statsCache = statsCache;
    this.objectMapper = objectMapper;
  }

  public Map<String, Object> collect(Map<String, Object> payload, VisitRequestContext context) {
    String type = text(payload.get("type"), "pageview");
    if (!type.equals("pageview") && !type.equals("duration") && !type.equals("event")) {
      throw new BusinessException(40060, "Unsupported collect type", HttpStatus.BAD_REQUEST);
    }
    String visitorId = visitorId(context.ip(), context.userAgent(), text(payload.get("screen"), ""), text(payload.get("language"), ""));
    long currentRate = statsCache.incrementCollectRate(visitorId, Duration.ofMinutes(1));
    if (currentRate > statsCache.collectRateLimitPerMinute()) {
      // 限流按匿名 visitor 维度执行，避免公开采集接口被单一客户端放大写入压力。
      throw new BusinessException(42901, "Too many collect requests", HttpStatus.TOO_MANY_REQUESTS);
    }
    Long articleId = number(payload.get("article_id"));
    visitRepository.save(
        new VisitEventInput(
            visitorId,
            type,
            text(payload.get("url"), ""),
            text(payload.get("hostname"), ""),
            text(payload.get("title"), ""),
            text(payload.get("referrer"), ""),
            text(payload.get("language"), ""),
            text(payload.get("screen"), ""),
            articleId,
            text(payload.get("event_name"), ""),
            writeJson(payload.get("event_data")),
            number(payload.get("duration")),
            context.ip(),
            context.userAgent(),
            timestamp(payload.get("timestamp"))));
    statsCache.invalidateSiteStats();
    boolean articleViewCounted = false;
    Long articleViewCount = null;
    if (type.equals("pageview") && articleId != null) {
      articleViewCounted =
          statsCache.markArticleViewIfAbsent(
              articleId, visitorId, Duration.ofSeconds(statsCache.articleViewDedupSeconds()));
      if (articleViewCounted) {
        // 浏览量只在去重窗口首次命中时递增，热度分也沿用同一口径。
        visitRepository.incrementPublishedArticleViewCount(articleId);
        statsCache.incrementHotArticle(articleId, 1);
      }
      articleViewCount = visitRepository.articleViewCount(articleId);
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

  private String visitorId(String ip, String userAgent, String screen, String language) {
    try {
      // visitorId 是匿名稳定标识，不保存可逆的客户端指纹。
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

  private String text(Object value, String fallback) {
    return value == null ? fallback : value.toString();
  }
}
