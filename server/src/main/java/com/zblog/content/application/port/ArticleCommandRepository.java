package com.zblog.content.application.port;

import java.util.List;
import java.util.Map;

public interface ArticleCommandRepository {

  Map<String, Object> create(
      String title,
      String slug,
      String markdown,
      String html,
      String text,
      String summary,
      String cover,
      Long categoryId,
	      List<Long> tagIds,
	      String location,
	      boolean top,
	      boolean essence,
	      boolean outdated,
	      String copyrightType,
	      String sourceUrl,
	      String sourceTitle,
	      String copyrightLicense);

  Map<String, Object> update(
      long id,
      String title,
      String slug,
      String markdown,
      String html,
      String text,
      String summary,
      String cover,
      Long categoryId,
	      List<Long> tagIds,
	      String location,
	      boolean top,
	      boolean essence,
	      boolean outdated,
	      String copyrightType,
	      String sourceUrl,
	      String sourceTitle,
	      String copyrightLicense);

  Map<String, Object> publish(long id);

  Map<String, Object> unpublish(long id);

  void delete(long id);
}
