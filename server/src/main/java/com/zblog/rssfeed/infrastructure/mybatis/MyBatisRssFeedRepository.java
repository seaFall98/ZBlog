package com.zblog.rssfeed.infrastructure.mybatis;

import com.zblog.common.exception.BusinessException;
import com.zblog.rssfeed.application.FeedItem;
import com.zblog.rssfeed.application.RssSource;
import com.zblog.rssfeed.application.port.RssFeedRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisRssFeedRepository implements RssFeedRepository {

  private final RssFeedMapper rssFeedMapper;

  public MyBatisRssFeedRepository(RssFeedMapper rssFeedMapper) {
    this.rssFeedMapper = rssFeedMapper;
  }

  public Map<String, Object> listAdmin(Map<String, String> params) {
    int page = number(params, "page", 1);
    int pageSize = number(params, "page_size", 20);
    int offset = Math.max(0, page - 1) * pageSize;
    String keyword = like(params.get("keyword"));
    Long friendId = longValue(params.get("friend_id"));
    Boolean read = booleanValue(params.get("is_read"));
    LocalDateTime start = parseStart(params.get("start_time"));
    LocalDateTime end = parseEnd(params.get("end_time"));
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("list", rssFeedMapper.listAdmin(keyword, friendId, read, start, end, pageSize, offset));
    result.put("total", rssFeedMapper.countAdmin(keyword, friendId, read, start, end));
    result.put("page", page);
    result.put("page_size", pageSize);
    result.put("unread_count", rssFeedMapper.unreadCount());
    return result;
  }

  public List<RssSource> listSources() {
    return rssFeedMapper.sourceRows().stream()
        .map(row -> new RssSource(number(row.get("id")), string(row.get("name")), string(row.get("rss_url"))))
        .toList();
  }

  public int insertItem(long friendId, FeedItem item) {
    if (!rssFeedMapper.articleIdsByFriendAndLink(friendId, item.link()).isEmpty()) {
      return 0;
    }
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("friendId", friendId);
    params.put("title", item.title());
    params.put("link", item.link());
    params.put("description", item.description());
    params.put("publishedAt", item.publishedAt());
    rssFeedMapper.insertItem(params);
    return 1;
  }

  public void markSourceSuccess(long friendId) {
    rssFeedMapper.markSourceSuccess(friendId);
  }

  public void markSourceFailed(long friendId, String errorMessage) {
    rssFeedMapper.markSourceFailed(friendId, errorMessage);
  }

  public void markRead(long id) {
    rssFeedMapper.markRead(id);
  }

  public int markAllRead() {
    return rssFeedMapper.markAllRead();
  }

  private int number(Map<String, String> params, String key, int fallback) {
    String value = params.get(key);
    if (value == null || value.isBlank()) {
      return fallback;
    }
    return Integer.parseInt(value);
  }

  private String like(String value) {
    String normalized = blankToNull(value);
    return normalized == null ? null : "%" + normalized.toLowerCase() + "%";
  }

  private Long longValue(String value) {
    String normalized = blankToNull(value);
    return normalized == null ? null : Long.parseLong(normalized);
  }

  private Boolean booleanValue(String value) {
    String normalized = blankToNull(value);
    return normalized == null ? null : Boolean.parseBoolean(normalized);
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private LocalDateTime parseStart(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return parseDate(value).atStartOfDay();
  }

  private LocalDateTime parseEnd(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return parseDate(value).plusDays(1).atStartOfDay();
  }

  private LocalDate parseDate(String value) {
    try {
      return LocalDate.parse(value);
    } catch (DateTimeParseException exception) {
      throw new BusinessException(40001, "Invalid date", HttpStatus.BAD_REQUEST);
    }
  }

  private long number(Object value) {
    return ((Number) value).longValue();
  }

  private String string(Object value) {
    return value == null ? "" : value.toString();
  }
}
