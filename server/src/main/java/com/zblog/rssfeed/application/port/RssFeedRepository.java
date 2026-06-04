package com.zblog.rssfeed.application.port;

import com.zblog.rssfeed.application.FeedItem;
import com.zblog.rssfeed.application.RssSource;
import java.util.List;
import java.util.Map;

public interface RssFeedRepository {

  Map<String, Object> listAdmin(Map<String, String> params);

  List<RssSource> listSources();

  int insertItem(long friendId, FeedItem item);

  void markSourceSuccess(long friendId);

  void markSourceFailed(long friendId, String errorMessage);

  void markRead(long id);

  int markAllRead();
}
