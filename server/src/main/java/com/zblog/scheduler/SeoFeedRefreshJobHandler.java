package com.zblog.scheduler;

import com.zblog.seo.application.SeoFeedService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SeoFeedRefreshJobHandler implements ScheduledJobHandler {

  private final SeoFeedService seoFeedService;

  public SeoFeedRefreshJobHandler(SeoFeedService seoFeedService) {
    this.seoFeedService = seoFeedService;
  }

  public String name() {
    return "seo-feed-refresh";
  }

  public String description() {
    return "Refresh cached RSS, Atom, and Sitemap XML.";
  }

  public String execute(Map<String, Object> parameters) {
    seoFeedService.refreshCache();
    return "Refreshed RSS, Atom, and Sitemap cache.";
  }
}
