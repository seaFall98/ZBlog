package com.zblog.event.application.port;

public interface ArticlePublishedNotifier {

  void notifyArticlePublished(long articleId, String title, String slug, long eventId);
}
