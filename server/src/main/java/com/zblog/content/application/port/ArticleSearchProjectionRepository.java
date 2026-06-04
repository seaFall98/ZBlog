package com.zblog.content.application.port;

import com.zblog.content.domain.ArticleSearchProjection;
import java.util.List;
import java.util.Optional;

public interface ArticleSearchProjectionRepository {

  Optional<ArticleSearchProjection> publishedSearchProjection(long articleId);

  List<ArticleSearchProjection> publishedSearchProjections();
}
