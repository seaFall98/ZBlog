package com.zblog.rssfeed;

import com.zblog.rssfeed.application.RssFeedAdminService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class RssFeedScheduler {

  private final RssFeedAdminService rssFeedAdminService;
  private final boolean enabled;

  public RssFeedScheduler(
      RssFeedAdminService rssFeedAdminService,
      @Value("${zblog.rss.scheduled-refresh-enabled:false}") boolean enabled) {
    this.rssFeedAdminService = rssFeedAdminService;
    this.enabled = enabled;
  }

  @Scheduled(fixedDelayString = "${zblog.rss.scheduled-refresh-fixed-delay-ms:3600000}")
  public void refreshFeeds() {
    if (!enabled) {
      return;
    }
    rssFeedAdminService.scheduledRefresh();
  }
}
