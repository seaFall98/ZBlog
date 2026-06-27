package com.zblog.scheduler;

import com.zblog.stats.application.ArticleViewCountBuffer;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ArticleViewFlushJobHandler implements ScheduledJobHandler {

  private final ArticleViewCountBuffer articleViewCountBuffer;

  public ArticleViewFlushJobHandler(ArticleViewCountBuffer articleViewCountBuffer) {
    this.articleViewCountBuffer = articleViewCountBuffer;
  }

  public String name() {
    return "article-view-flush";
  }

  public String description() {
    return "Flush pending Redis article view-count deltas to the database.";
  }

  public String execute(Map<String, Object> parameters) {
    Map<Long, Long> flushed = articleViewCountBuffer.flush();
    long total = flushed.values().stream().mapToLong(Long::longValue).sum();
    return "Flushed " + total + " article views across " + flushed.size() + " articles.";
  }
}
