package com.zblog.content.application.port;

import com.zblog.content.domain.ArticleSearchProjection;
import java.util.Map;

public interface ArticleEventPublisher {

  // Content 只声明业务事件语义，具体 outbox、MQ 或同步消费由 adapter 决定。
  void articlePublished(Map<String, Object> article);

  void articleSearchUpsert(ArticleSearchProjection projection);

  void articleSearchDelete(long articleId);
}
