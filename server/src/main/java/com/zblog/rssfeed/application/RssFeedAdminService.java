package com.zblog.rssfeed.application;

import com.zblog.common.exception.BusinessException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@Service
public class RssFeedAdminService {

  private static final int MAX_FEED_BYTES = 1024 * 1024;

  private final JdbcTemplate jdbcTemplate;
  private final HttpClient httpClient;
  private final boolean allowPrivateNetwork;

  public RssFeedAdminService(
      JdbcTemplate jdbcTemplate,
      @Value("${zblog.rss.allow-private-network:false}") boolean allowPrivateNetwork) {
    this.jdbcTemplate = jdbcTemplate;
    this.allowPrivateNetwork = allowPrivateNetwork;
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
  }

  public Map<String, Object> listAdmin(Map<String, String> params) {
    int page = number(params, "page", 1);
    int pageSize = number(params, "page_size", 20);
    int offset = Math.max(0, page - 1) * pageSize;
    List<Object> args = new ArrayList<>();
    String where = buildWhere(params, args);
    Long total =
        jdbcTemplate.queryForObject(
            "select count(*) from rss_feed_articles r left join friends f on f.id = r.friend_id " + where,
            Long.class,
            args.toArray());
    Long unreadCount =
        jdbcTemplate.queryForObject("select count(*) from rss_feed_articles where is_read = false", Long.class);
    List<Object> listArgs = new ArrayList<>(args);
    listArgs.add(pageSize);
    listArgs.add(offset);
    List<Map<String, Object>> list =
        jdbcTemplate.query(
            """
            select r.id, r.friend_id, coalesce(f.name, '') as friend_name, coalesce(f.url, '') as friend_url,
              r.title, r.link, r.description, r.published_at, r.is_read, r.created_at
            from rss_feed_articles r
            left join friends f on f.id = r.friend_id
            """
                + where
                + " order by coalesce(r.published_at, r.created_at) desc, r.id desc limit ? offset ?",
            (rs, rowNum) -> mapRow(rs),
            listArgs.toArray());
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("list", list);
    result.put("total", total == null ? 0 : total);
    result.put("page", page);
    result.put("page_size", pageSize);
    result.put("unread_count", unreadCount == null ? 0 : unreadCount);
    return result;
  }

  @Transactional
  public Map<String, Object> refresh() {
    List<Map<String, Object>> sources =
        jdbcTemplate.queryForList(
            """
            select id, name, rss_url
            from friends
            where rss_url is not null and rss_url <> '' and is_pending = false
            order by id
            """);
    int fetched = 0;
    int inserted = 0;
    int failed = 0;
    List<Map<String, Object>> results = new ArrayList<>();
    for (Map<String, Object> source : sources) {
      long friendId = ((Number) source.get("id")).longValue();
      String rssUrl = source.get("rss_url").toString();
      try {
        List<FeedItem> items = fetchItems(rssUrl);
        int sourceInserted = 0;
        for (FeedItem item : items) {
          sourceInserted += insertItem(friendId, item);
        }
        fetched++;
        inserted += sourceInserted;
        jdbcTemplate.update(
            """
            update friends
            set rss_status = 'success', rss_last_fetch_at = current_timestamp, rss_last_error = '', updated_at = current_timestamp
            where id = ?
            """,
            friendId);
        results.add(Map.of("friend_id", friendId, "status", "success", "inserted", sourceInserted));
      } catch (RuntimeException exception) {
        failed++;
        String message = exception.getMessage() == null ? "RSS fetch failed" : exception.getMessage();
        jdbcTemplate.update(
            """
            update friends
            set rss_status = 'failed', rss_last_fetch_at = current_timestamp, rss_last_error = ?, updated_at = current_timestamp
            where id = ?
            """,
            message,
            friendId);
        results.add(Map.of("friend_id", friendId, "status", "failed", "error", message));
      }
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("sources", sources.size());
    result.put("fetched", fetched);
    result.put("inserted", inserted);
    result.put("failed", failed);
    result.put("results", results);
    return result;
  }

  public void markRead(long id) {
    jdbcTemplate.update("update rss_feed_articles set is_read = true where id = ?", id);
  }

  public Map<String, Object> markAllRead() {
    int affected = jdbcTemplate.update("update rss_feed_articles set is_read = true where is_read = false");
    return Map.of("affected", affected);
  }

  private List<FeedItem> fetchItems(String value) {
    URI uri = validatedHttpUri(value);
    HttpRequest request =
        HttpRequest.newBuilder(uri)
            .GET()
            .timeout(Duration.ofSeconds(10))
            .header("User-Agent", "ZBlogRssReader/0.1")
            .build();
    try {
      HttpResponse<InputStream> response =
          httpClient.send(request, responseInfo -> HttpResponse.BodySubscribers.ofInputStream());
      byte[] body = readLimited(response.body());
      int status = response.statusCode();
      if (status < 200 || status >= 300) {
        throw new IllegalStateException("HTTP " + status);
      }
      return parseFeed(body, uri);
    } catch (RuntimeException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalStateException("RSS fetch failed");
    }
  }

  private List<FeedItem> parseFeed(byte[] body, URI feedUri) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setXIncludeAware(false);
      factory.setExpandEntityReferences(false);
      var document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(body));
      List<FeedItem> items = new ArrayList<>();
      var rssItems = document.getElementsByTagName("item");
      for (int i = 0; i < rssItems.getLength(); i++) {
        items.add(parseRssItem((Element) rssItems.item(i), feedUri));
      }
      var atomEntries = document.getElementsByTagName("entry");
      for (int i = 0; i < atomEntries.getLength(); i++) {
        items.add(parseAtomEntry((Element) atomEntries.item(i), feedUri));
      }
      return items.stream().filter(item -> !item.title().isBlank() && !item.link().isBlank()).toList();
    } catch (Exception exception) {
      throw new IllegalStateException("RSS parse failed");
    }
  }

  private FeedItem parseRssItem(Element element, URI feedUri) {
    String title = childText(element, "title");
    String link = absoluteUrl(feedUri, childText(element, "link"));
    String description = childText(element, "description");
    LocalDateTime publishedAt = parsePublishedAt(childText(element, "pubDate"));
    return new FeedItem(title, link, description, publishedAt);
  }

  private FeedItem parseAtomEntry(Element element, URI feedUri) {
    String title = childText(element, "title");
    String link = "";
    var links = element.getElementsByTagName("link");
    for (int i = 0; i < links.getLength(); i++) {
      Element linkElement = (Element) links.item(i);
      String rel = linkElement.getAttribute("rel");
      if (rel.isBlank() || "alternate".equals(rel)) {
        link = linkElement.getAttribute("href");
        break;
      }
    }
    String description = childText(element, "summary");
    if (description.isBlank()) {
      description = childText(element, "content");
    }
    String published = childText(element, "published");
    if (published.isBlank()) {
      published = childText(element, "updated");
    }
    return new FeedItem(title, absoluteUrl(feedUri, link), description, parsePublishedAt(published));
  }

  private int insertItem(long friendId, FeedItem item) {
    List<Long> existing =
        jdbcTemplate.query(
            "select id from rss_feed_articles where friend_id = ? and link = ?",
            (rs, rowNum) -> rs.getLong("id"),
            friendId,
            item.link());
    if (!existing.isEmpty()) {
      return 0;
    }
    jdbcTemplate.update(
        """
        insert into rss_feed_articles (friend_id, title, link, description, published_at, is_read)
        values (?, ?, ?, ?, ?, false)
        """,
        friendId,
        item.title(),
        item.link(),
        item.description(),
        item.publishedAt() == null ? null : Timestamp.valueOf(item.publishedAt()));
    return 1;
  }

  private URI validatedHttpUri(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("RSS URL is required");
    }
    try {
      URI uri = URI.create(value.trim()).normalize();
      if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
        throw new IllegalStateException("Only http and https RSS URLs are supported");
      }
      String host = uri.getHost();
      if (host == null || host.isBlank()) {
        throw new IllegalStateException("Invalid RSS URL host");
      }
      String asciiHost = IDN.toASCII(host);
      if (!allowPrivateNetwork && isPrivateHost(asciiHost)) {
        throw new IllegalStateException("Private network RSS URLs are not allowed");
      }
      return new URI(
          uri.getScheme(),
          uri.getUserInfo(),
          asciiHost,
          uri.getPort(),
          uri.getPath(),
          uri.getQuery(),
          uri.getFragment());
    } catch (IllegalStateException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalStateException("Invalid RSS URL");
    }
  }

  private byte[] readLimited(InputStream input) throws Exception {
    byte[] body = input.readNBytes(MAX_FEED_BYTES + 1);
    if (body.length > MAX_FEED_BYTES) {
      throw new IllegalStateException("RSS feed is too large");
    }
    return body;
  }

  private boolean isPrivateHost(String host) throws Exception {
    InetAddress address = InetAddress.getByName(host);
    return address.isAnyLocalAddress()
        || address.isLoopbackAddress()
        || address.isLinkLocalAddress()
        || address.isSiteLocalAddress()
        || address.isMulticastAddress();
  }

  private String childText(Element element, String tag) {
    var nodes = element.getElementsByTagName(tag);
    if (nodes.getLength() == 0) {
      return "";
    }
    Node node = nodes.item(0);
    return node == null || node.getTextContent() == null ? "" : decodeHtml(node.getTextContent().trim());
  }

  private String absoluteUrl(URI feedUri, String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return feedUri.resolve(value.trim()).toString();
  }

  private LocalDateTime parsePublishedAt(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return OffsetDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
          .withOffsetSameInstant(ZoneOffset.UTC)
          .toLocalDateTime();
    } catch (DateTimeParseException ignored) {
      try {
        return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
      } catch (DateTimeParseException ignoredAgain) {
        return null;
      }
    }
  }

  private String decodeHtml(String value) {
    return value
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'");
  }

  private String buildWhere(Map<String, String> params, List<Object> args) {
    StringBuilder where = new StringBuilder("where 1 = 1");
    String keyword = params.get("keyword");
    if (keyword != null && !keyword.isBlank()) {
      where.append(" and (lower(r.title) like ? or lower(r.description) like ? or lower(f.name) like ?)");
      String like = "%" + keyword.toLowerCase() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
    }
    String friendId = params.get("friend_id");
    if (friendId != null && !friendId.isBlank()) {
      where.append(" and r.friend_id = ?");
      args.add(Long.parseLong(friendId));
    }
    String read = params.get("is_read");
    if (read != null && !read.isBlank()) {
      where.append(" and r.is_read = ?");
      args.add(Boolean.parseBoolean(read));
    }
    addTimeRange(where, args, params, "r.published_at");
    return where.toString();
  }

  private void addTimeRange(
      StringBuilder where, List<Object> args, Map<String, String> params, String column) {
    LocalDateTime start = parseStart(params.get("start_time"));
    LocalDateTime end = parseEnd(params.get("end_time"));
    if (start != null) {
      where.append(" and ").append(column).append(" >= ?");
      args.add(Timestamp.valueOf(start));
    }
    if (end != null) {
      where.append(" and ").append(column).append(" < ?");
      args.add(Timestamp.valueOf(end));
    }
  }

  private Map<String, Object> mapRow(ResultSet rs) throws SQLException {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", rs.getLong("id"));
    row.put("friend_id", rs.getLong("friend_id"));
    row.put("friend_name", rs.getString("friend_name"));
    row.put("friend_url", rs.getString("friend_url"));
    row.put("title", rs.getString("title"));
    row.put("link", rs.getString("link"));
    row.put("description", rs.getString("description"));
    row.put("published_at", rs.getTimestamp("published_at"));
    row.put("is_read", rs.getBoolean("is_read"));
    row.put("created_at", rs.getTimestamp("created_at"));
    return row;
  }

  private record FeedItem(
      String title, String link, String description, LocalDateTime publishedAt) {}

  private int number(Map<String, String> params, String key, int fallback) {
    String value = params.get(key);
    if (value == null || value.isBlank()) {
      return fallback;
    }
    return Integer.parseInt(value);
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
}
