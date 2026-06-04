package com.zblog.rssfeed.application;

import com.zblog.rssfeed.application.port.RssFeedFetcher;
import com.zblog.rssfeed.application.port.RssFeedRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RssFeedAdminService {

  private final RssFeedRepository rssFeedRepository;
  private final RssFeedFetcher rssFeedFetcher;

  public RssFeedAdminService(RssFeedRepository rssFeedRepository, RssFeedFetcher rssFeedFetcher) {
    this.rssFeedRepository = rssFeedRepository;
    this.rssFeedFetcher = rssFeedFetcher;
  }

  public Map<String, Object> listAdmin(Map<String, String> params) {
    return rssFeedRepository.listAdmin(params);
  }

  @Transactional
  public Map<String, Object> scheduledRefresh() {
    return refresh();
  }

  @Transactional
  public Map<String, Object> refresh() {
    List<RssSource> sources = rssFeedRepository.listSources();
    int fetched = 0;
    int inserted = 0;
    int failed = 0;
    List<Map<String, Object>> results = new ArrayList<>();
    for (RssSource source : sources) {
      try {
        List<FeedItem> items = rssFeedFetcher.fetchItems(source.rssUrl());
        int sourceInserted = 0;
        for (FeedItem item : items) {
          sourceInserted += rssFeedRepository.insertItem(source.id(), item);
        }
        fetched++;
        inserted += sourceInserted;
        rssFeedRepository.markSourceSuccess(source.id());
        results.add(Map.of("friend_id", source.id(), "status", "success", "inserted", sourceInserted));
      } catch (RuntimeException exception) {
        failed++;
        String message = exception.getMessage() == null ? "RSS fetch failed" : exception.getMessage();
        rssFeedRepository.markSourceFailed(source.id(), message);
        results.add(Map.of("friend_id", source.id(), "status", "failed", "error", message));
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
    rssFeedRepository.markRead(id);
  }

  public Map<String, Object> markAllRead() {
    int affected = rssFeedRepository.markAllRead();
    return Map.of("affected", affected);
  }
}
