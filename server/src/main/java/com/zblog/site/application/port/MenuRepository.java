package com.zblog.site.application.port;

import com.zblog.site.domain.Menu;
import java.util.List;

public interface MenuRepository {

  List<Menu> findAll();

  Menu get(long id);

  Menu create(String type, Long parentId, String title, String url, String icon, int sort);

  Menu update(long id, String type, Long parentId, String title, String url, String icon, int sort);

  void delete(long id, boolean deleteChildren);

  void deleteByTypes(List<String> types);
}
