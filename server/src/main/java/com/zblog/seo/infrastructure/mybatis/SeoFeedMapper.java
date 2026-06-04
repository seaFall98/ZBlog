package com.zblog.seo.infrastructure.mybatis;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SeoFeedMapper {

  List<Map<String, Object>> publishedFeedArticles();
}
