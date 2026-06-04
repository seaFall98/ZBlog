package com.zblog.moment.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.api.PageResponse;
import com.zblog.moment.application.port.MomentRepository;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MomentService {

  private static final TypeReference<LinkedHashMap<String, Object>> CONTENT_TYPE =
      new TypeReference<>() {};

  private final MomentRepository momentRepository;
  private final ObjectMapper objectMapper;

  public MomentService(MomentRepository momentRepository, ObjectMapper objectMapper) {
    this.momentRepository = momentRepository;
    this.objectMapper = objectMapper;
  }

  public PageResponse<Map<String, Object>> listPublic(int page, int pageSize) {
    return paginate(
        loadAll().stream().filter(MomentEntry::isPublish).sorted(order().reversed()).toList(),
        page,
        pageSize);
  }

  public PageResponse<Map<String, Object>> listAdmin(
      int page,
      int pageSize,
      String keyword,
      String tags,
      String location,
      Boolean isPublish,
      Boolean hasImages,
      Boolean hasVideo,
      Boolean hasAudio,
      Boolean hasMusic,
      Boolean hasLink,
      String startTime,
      String endTime) {
    List<MomentEntry> filtered = new ArrayList<>(loadAll());
    filtered.removeIf(entry -> !matches(entry, keyword, tags, location, isPublish, hasImages, hasVideo, hasAudio, hasMusic, hasLink, startTime, endTime));
    filtered.sort(order().reversed());
    return paginate(filtered, page, pageSize);
  }

  public Map<String, Object> get(long id) {
    return view(find(id));
  }

  public Map<String, Object> create(Map<String, Object> request) {
    MomentInput input = normalizeInput(request, null);
    return get(momentRepository.create(input.contentJson(), input.isPublish(), input.publishTime()));
  }

  public Map<String, Object> update(long id, Map<String, Object> request) {
    Map<String, Object> existing = get(id);
    MomentInput input = normalizeInput(request, existing);
    momentRepository.update(id, input.contentJson(), input.isPublish(), input.publishTime());
    return get(id);
  }

  public void delete(long id) {
    momentRepository.delete(id);
  }

  private PageResponse<Map<String, Object>> paginate(List<MomentEntry> entries, int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.max(pageSize, 1);
    int total = entries.size();
    int fromIndex = Math.min((normalizedPage - 1) * normalizedPageSize, total);
    int toIndex = Math.min(fromIndex + normalizedPageSize, total);
    List<Map<String, Object>> list =
        entries.subList(fromIndex, toIndex).stream().map(this::view).toList();
    return new PageResponse<>(list, total, normalizedPage, normalizedPageSize);
  }

  private List<MomentEntry> loadAll() {
    return momentRepository.findAll().stream().map(this::toEntry).toList();
  }

  private MomentEntry find(long id) {
    return toEntry(momentRepository.find(id));
  }

  private MomentEntry toEntry(Map<String, Object> row) {
    Map<String, Object> content = toContentMap(row.get("content_json"));
    Instant publishTime = timestamp(row.get("publish_time"));
    Instant createdAt = timestamp(row.get("created_at"));
    Instant updatedAt = timestamp(row.get("updated_at"));
    String searchableText = searchableText(content);
    String tagsText = stringValue(content, "tags");
    String locationText = stringValue(content, "location");
    return new MomentEntry(
        number(row.get("id")).longValue(),
        content,
        booleanValue(row.get("is_publish"), true),
        publishTime == null ? now() : publishTime,
        createdAt == null ? now() : createdAt,
        updatedAt == null ? now() : updatedAt,
        searchableText,
        tagsText,
        locationText,
        hasMeaningfulValue(content.get("images")),
        hasMeaningfulValue(content.get("video")),
        hasMeaningfulValue(content.get("audio")),
        hasMeaningfulValue(content.get("music")),
        hasMeaningfulValue(content.get("link")));
  }

  private Map<String, Object> view(MomentEntry entry) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", entry.id());
    view.put("content", entry.content());
    view.put("is_publish", entry.isPublish());
    view.put("publish_time", entry.publishTime().toString());
    view.put("created_at", entry.createdAt().toString());
    view.put("updated_at", entry.updatedAt().toString());
    return view;
  }

  private MomentInput normalizeInput(Map<String, Object> request, Map<String, Object> existing) {
    Map<String, Object> baseContent = existing == null ? new LinkedHashMap<>() : content(existing.get("content"));
    Map<String, Object> content = request.containsKey("content") ? content(request.get("content")) : baseContent;
    boolean isPublish =
        request.containsKey("is_publish")
            ? booleanValue(request.get("is_publish"), true)
            : existing == null || booleanValue(existing.get("is_publish"), true);
    Instant publishTime =
        request.containsKey("publish_time")
            ? timestamp(request.get("publish_time"))
            : existing == null
                ? null
                : timestamp(existing.get("publish_time"));
    if (publishTime == null) {
      publishTime = now();
    }
    return new MomentInput(writeContent(content), isPublish, publishTime);
  }

  private List<MomentEntry> filter(
      List<MomentEntry> entries,
      String keyword,
      String tags,
      String location,
      Boolean isPublish,
      Boolean hasImages,
      Boolean hasVideo,
      Boolean hasAudio,
      Boolean hasMusic,
      Boolean hasLink,
      String startTime,
      String endTime) {
    String normalizedKeyword = normalize(keyword);
    String normalizedTags = normalize(tags);
    String normalizedLocation = normalize(location);
    LocalDate start = parseDate(startTime);
    LocalDate end = parseDate(endTime);

    return entries.stream()
        .filter(
            entry -> {
              if (isPublish != null && entry.isPublish() != isPublish) {
                return false;
              }
              if (!normalizedKeyword.isBlank() && !entry.searchableText().contains(normalizedKeyword)) {
                return false;
              }
              if (!normalizedTags.isBlank() && !normalize(entry.tagsText()).contains(normalizedTags)) {
                return false;
              }
              if (!normalizedLocation.isBlank()
                  && !normalize(entry.locationText()).contains(normalizedLocation)) {
                return false;
              }
              if (hasImages != null && entry.hasImages() != hasImages) {
                return false;
              }
              if (hasVideo != null && entry.hasVideo() != hasVideo) {
                return false;
              }
              if (hasAudio != null && entry.hasAudio() != hasAudio) {
                return false;
              }
              if (hasMusic != null && entry.hasMusic() != hasMusic) {
                return false;
              }
              if (hasLink != null && entry.hasLink() != hasLink) {
                return false;
              }
              LocalDate publishDate =
                  entry.publishTime().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
              if (start != null && publishDate.isBefore(start)) {
                return false;
              }
              if (end != null && publishDate.isAfter(end)) {
                return false;
              }
              return true;
            })
        .toList();
  }

  private boolean matches(
      MomentEntry entry,
      String keyword,
      String tags,
      String location,
      Boolean isPublish,
      Boolean hasImages,
      Boolean hasVideo,
      Boolean hasAudio,
      Boolean hasMusic,
      Boolean hasLink,
      String startTime,
      String endTime) {
    return filter(
            List.of(entry),
            keyword,
            tags,
            location,
            isPublish,
            hasImages,
            hasVideo,
            hasAudio,
            hasMusic,
            hasLink,
            startTime,
            endTime)
        .size()
        > 0;
  }

  private Comparator<MomentEntry> order() {
    return Comparator.comparing(MomentEntry::publishTime).thenComparingLong(MomentEntry::id);
  }

  private Map<String, Object> content(Object value) {
    if (value == null) {
      return new LinkedHashMap<>();
    }
    if (value instanceof String text) {
      if (text.isBlank()) {
        return new LinkedHashMap<>();
      }
      try {
        return objectMapper.readValue(text, CONTENT_TYPE);
      } catch (Exception exception) {
        throw new IllegalStateException("Unable to parse moment content", exception);
      }
    }
    return objectMapper.convertValue(value, CONTENT_TYPE);
  }

  private Map<String, Object> toContentMap(Object value) {
    if (value == null) {
      return new LinkedHashMap<>();
    }
    if (value instanceof String text) {
      if (text.isBlank()) {
        return new LinkedHashMap<>();
      }
      try {
        return objectMapper.readValue(text, CONTENT_TYPE);
      } catch (Exception exception) {
        throw new IllegalStateException("Unable to parse moment content", exception);
      }
    }
    return objectMapper.convertValue(value, CONTENT_TYPE);
  }

  private String writeContent(Map<String, Object> content) {
    try {
      return objectMapper.writeValueAsString(content);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize moment content", exception);
    }
  }

  private Instant timestamp(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Instant instant) {
      return instant;
    }
    if (value instanceof java.util.Date date) {
      return date.toInstant();
    }
    if (value instanceof LocalDateTime localDateTime) {
      return localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
    }
    if (value instanceof OffsetDateTime offsetDateTime) {
      return offsetDateTime.toInstant();
    }
    if (value instanceof String text && !text.isBlank()) {
      try {
        return LocalDateTime.parse(text.replace(' ', 'T'))
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant();
      } catch (DateTimeParseException exception) {
        try {
          return OffsetDateTime.parse(text).toInstant();
        } catch (DateTimeParseException ignored) {
          return Instant.parse(text);
        }
      }
    }
    return null;
  }

  private Instant now() {
    return Instant.now();
  }

  private LocalDate parseDate(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value);
    } catch (DateTimeException exception) {
      return null;
    }
  }

  private String searchableText(Map<String, Object> content) {
    StringBuilder builder = new StringBuilder();
    appendText(builder, content);
    return builder.toString().toLowerCase();
  }

  private void appendText(StringBuilder builder, Object value) {
    if (value == null) {
      return;
    }
    if (value instanceof Map<?, ?> map) {
      map.values().forEach(item -> appendText(builder, item));
      return;
    }
    if (value instanceof Iterable<?> iterable) {
      iterable.forEach(item -> appendText(builder, item));
      return;
    }
    builder.append(' ').append(value);
  }

  private boolean hasMeaningfulValue(Object value) {
    if (value == null) {
      return false;
    }
    if (value instanceof String text) {
      return !text.isBlank();
    }
    if (value instanceof Map<?, ?> map) {
      return map.values().stream().anyMatch(this::hasMeaningfulValue);
    }
    if (value instanceof Iterable<?> iterable) {
      return iterable.iterator().hasNext();
    }
    return true;
  }

  private String stringValue(Map<String, Object> map, String key) {
    Object value = map.get(key);
    return value == null ? "" : value.toString();
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase();
  }

  private Number number(Object value) {
    if (value instanceof Number number) {
      return number;
    }
    if (value instanceof String text && !text.isBlank()) {
      return Long.parseLong(text);
    }
    throw new IllegalStateException("Moment id is missing");
  }

  private boolean booleanValue(Object value, boolean fallback) {
    if (value instanceof Boolean bool) {
      return bool;
    }
    if (value instanceof String text && !text.isBlank()) {
      return Boolean.parseBoolean(text);
    }
    return fallback;
  }


  private record MomentEntry(
      long id,
      Map<String, Object> content,
      boolean isPublish,
      Instant publishTime,
      Instant createdAt,
      Instant updatedAt,
      String searchableText,
      String tagsText,
      String locationText,
      boolean hasImages,
      boolean hasVideo,
      boolean hasAudio,
      boolean hasMusic,
      boolean hasLink) {}

  private record MomentInput(String contentJson, boolean isPublish, Instant publishTime) {}
}
