package com.zblog.site.infrastructure;

import com.zblog.common.exception.BusinessException;
import com.zblog.site.domain.Menu;
import java.sql.PreparedStatement;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMenuRepository {

  private final JdbcClient jdbcClient;
  private final JdbcTemplate jdbcTemplate;

  public JdbcMenuRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
    this.jdbcClient = jdbcClient;
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Menu> findAll() {
    return jdbcClient
        .sql(
            """
            select id, type, parent_id, title, url, icon, sort_order
            from menus
            order by type, sort_order, id
            """)
        .query(this::mapRow)
        .list();
  }

  public Menu get(long id) {
    List<Menu> rows =
        jdbcClient
            .sql(
                """
                select id, type, parent_id, title, url, icon, sort_order
                from menus
                where id = :id
                """)
            .param("id", id)
            .query(this::mapRow)
            .list();
    if (rows.isEmpty()) {
      throw new BusinessException(404, "Menu not found", HttpStatus.NOT_FOUND);
    }
    return rows.getFirst();
  }

  public Menu create(String type, Long parentId, String title, String url, String icon, int sort) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  """
                  insert into menus (type, parent_id, title, url, icon, sort_order)
                  values (?, ?, ?, ?, ?, ?)
                  """,
                  new String[] {"id"});
          statement.setString(1, type);
          if (parentId == null) {
            statement.setObject(2, null);
          } else {
            statement.setLong(2, parentId);
          }
          statement.setString(3, title);
          statement.setString(4, url);
          statement.setString(5, icon);
          statement.setInt(6, sort);
          return statement;
        },
        keyHolder);
    return get(keyHolder.getKey().longValue());
  }

  public Menu update(
      long id, String type, Long parentId, String title, String url, String icon, int sort) {
    jdbcTemplate.update(
        """
        update menus
        set type = ?, parent_id = ?, title = ?, url = ?, icon = ?, sort_order = ?
        where id = ?
        """,
        type,
        parentId,
        title,
        url,
        icon,
        sort,
        id);
    return get(id);
  }

  public void delete(long id, boolean deleteChildren) {
    if (deleteChildren) {
      jdbcTemplate.update("delete from menus where parent_id = ?", id);
    } else {
      jdbcTemplate.update("update menus set parent_id = null where parent_id = ?", id);
    }
    jdbcTemplate.update("delete from menus where id = ?", id);
  }

  private Menu mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
    return new Menu(
        rs.getLong("id"),
        rs.getString("type"),
        rs.getObject("parent_id", Long.class),
        rs.getString("title"),
        rs.getString("url"),
        rs.getString("icon"),
        rs.getInt("sort_order"));
  }
}
