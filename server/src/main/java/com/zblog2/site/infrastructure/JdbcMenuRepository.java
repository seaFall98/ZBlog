package com.zblog2.site.infrastructure;

import com.zblog2.site.domain.Menu;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMenuRepository {

  private final JdbcClient jdbcClient;

  public JdbcMenuRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public List<Menu> findAll() {
    return jdbcClient
        .sql(
            """
            select id, type, parent_id, title, url, icon, sort_order
            from menus
            order by type, sort_order, id
            """)
        .query(
            (rs, rowNum) ->
                new Menu(
                    rs.getLong("id"),
                    rs.getString("type"),
                    rs.getObject("parent_id", Long.class),
                    rs.getString("title"),
                    rs.getString("url"),
                    rs.getString("icon"),
                    rs.getInt("sort_order")))
        .list();
  }
}
