package com.zblog.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zblog.cache")
public class CacheProperties {

  private int articleViewDedupSeconds = 5;
  private int collectRateLimitPerMinute = 120;
  private int siteStatsCacheSeconds = 60;

  public int getArticleViewDedupSeconds() {
    return articleViewDedupSeconds;
  }

  public void setArticleViewDedupSeconds(int articleViewDedupSeconds) {
    this.articleViewDedupSeconds = articleViewDedupSeconds;
  }

  public int getCollectRateLimitPerMinute() {
    return collectRateLimitPerMinute;
  }

  public void setCollectRateLimitPerMinute(int collectRateLimitPerMinute) {
    this.collectRateLimitPerMinute = collectRateLimitPerMinute;
  }

  public int getSiteStatsCacheSeconds() {
    return siteStatsCacheSeconds;
  }

  public void setSiteStatsCacheSeconds(int siteStatsCacheSeconds) {
    this.siteStatsCacheSeconds = siteStatsCacheSeconds;
  }
}
