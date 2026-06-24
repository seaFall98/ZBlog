package com.zblog.site.infrastructure.mybatis;

import com.zblog.common.exception.BusinessException;
import com.zblog.site.application.port.MenuRepository;
import com.zblog.site.domain.Menu;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisMenuRepository implements MenuRepository {

  private final MenuMapper menuMapper;

  public MyBatisMenuRepository(MenuMapper menuMapper) {
    this.menuMapper = menuMapper;
  }

  public List<Menu> findAll() {
    return menuMapper.findAllRows().stream().map(this::mapRow).toList();
  }

  public Menu get(long id) {
    List<Map<String, Object>> rows = menuMapper.rowsById(id);
    if (rows.isEmpty()) {
      throw new BusinessException(404, "Menu not found", HttpStatus.NOT_FOUND);
    }
    return mapRow(rows.getFirst());
  }

  public Menu create(String type, Long parentId, String title, String url, String icon, int sort) {
    Map<String, Object> params = menuParams(0, type, parentId, title, url, icon, sort);
    menuMapper.insertMenu(params);
    return get(generatedId(params));
  }

  public Menu update(long id, String type, Long parentId, String title, String url, String icon, int sort) {
    menuMapper.updateMenu(menuParams(id, type, parentId, title, url, icon, sort));
    return get(id);
  }

  public void delete(long id, boolean deleteChildren) {
    if (deleteChildren) {
      menuMapper.deleteChildren(id);
    } else {
      menuMapper.detachChildren(id);
    }
    menuMapper.deleteMenu(id);
  }

  public void deleteByTypes(List<String> types) {
    if (types.isEmpty()) {
      return;
    }
    menuMapper.deleteByTypes(types);
  }

  private Map<String, Object> menuParams(
      long id, String type, Long parentId, String title, String url, String icon, int sort) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("id", id);
    params.put("type", type);
    params.put("parentId", parentId);
    params.put("title", title);
    params.put("url", url);
    params.put("icon", icon);
    params.put("sort", sort);
    return params;
  }

  private Menu mapRow(Map<String, Object> row) {
    return new Menu(
        number(row.get("id")),
        row.get("type").toString(),
        nullableLong(row.get("parent_id")),
        row.get("title").toString(),
        string(row.get("url")),
        string(row.get("icon")),
        (int) number(row.get("sort_order")));
  }

  private long generatedId(Map<String, Object> params) {
    return number(params.get("id"));
  }

  private Long nullableLong(Object value) {
    return value instanceof Number number ? number.longValue() : null;
  }

  private long number(Object value) {
    return ((Number) value).longValue();
  }

  private String string(Object value) {
    return value == null ? "" : value.toString();
  }
}
