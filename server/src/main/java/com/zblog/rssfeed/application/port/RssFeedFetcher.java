package com.zblog.rssfeed.application.port;

import com.zblog.rssfeed.application.FeedItem;
import java.util.List;

public interface RssFeedFetcher {

  List<FeedItem> fetchItems(String rssUrl);
}
