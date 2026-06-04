package com.zblog.album.infrastructure;

import com.zblog.album.application.port.AlbumRepository;
import com.zblog.common.api.PageResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcAlbumRepository implements AlbumRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcAlbumRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public PageResponse<Map<String, Object>> listPublicRows(int page, int pageSize) {
    int offset = Math.max(0, page - 1) * pageSize;
    Long total = jdbcTemplate.queryForObject("select count(*) from albums where is_public = true", Long.class);
    List<Map<String, Object>> list =
        jdbcTemplate.queryForList(
            """
            select a.*, coalesce(p.photo_count, 0) as photo_count
            from albums a
            left join (
              select album_id, count(*) as photo_count
              from album_photos
              where is_public = true
              group by album_id
            ) p on p.album_id = a.id
            where a.is_public = true
            order by a.sort_order asc, a.created_at desc, a.id desc
            limit ? offset ?
            """,
            pageSize,
            offset);
    return new PageResponse<>(list, total == null ? 0 : total, page, pageSize);
  }

  public Map<String, Object> findPublicBySlug(String slug) {
    List<Map<String, Object>> rows =
        jdbcTemplate.queryForList("select * from albums where slug = ? and is_public = true", slug);
    return rows.isEmpty() ? null : rows.getFirst();
  }

  public PageResponse<Map<String, Object>> listAdminRows(
      int page, int pageSize, String keyword, Boolean isPublic) {
    int offset = Math.max(0, page - 1) * pageSize;
    List<Object> args = new ArrayList<>();
    StringBuilder where = new StringBuilder(" where 1 = 1 ");
    if (keyword != null && !keyword.isBlank()) {
      where.append(
          " and (lower(a.title) like ? or lower(a.slug) like ? or lower(coalesce(a.description, '')) like ?) ");
      String like = "%" + keyword.trim().toLowerCase() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
    }
    if (isPublic != null) {
      where.append(" and a.is_public = ? ");
      args.add(isPublic);
    }
    Long total =
        jdbcTemplate.queryForObject("select count(*) from albums a" + where, Long.class, args.toArray());
    args.add(pageSize);
    args.add(offset);
    List<Map<String, Object>> list =
        jdbcTemplate.queryForList(
            """
            select a.*, coalesce(p.photo_count, 0) as photo_count
            from albums a
            left join (
              select album_id, count(*) as photo_count
              from album_photos
              group by album_id
            ) p on p.album_id = a.id
            """
                + where
                + """
            order by a.sort_order asc, a.created_at desc, a.id desc
            limit ? offset ?
            """,
            args.toArray());
    return new PageResponse<>(list, total == null ? 0 : total, page, pageSize);
  }

  public Map<String, Object> findAlbum(long id) {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList("select * from albums where id = ?", id);
    return rows.isEmpty() ? null : rows.getFirst();
  }

  public long createAlbum(
      String title,
      String slug,
      String description,
      String coverUrl,
      int sortOrder,
      boolean isPublic) {
    return insertAndReturnId(
        """
        insert into albums (title, slug, description, cover_url, sort_order, is_public)
        values (?, ?, ?, ?, ?, ?)
        """,
        title,
        slug,
        description,
        coverUrl,
        sortOrder,
        isPublic);
  }

  public void updateAlbum(
      long id,
      String title,
      String slug,
      String description,
      String coverUrl,
      int sortOrder,
      boolean isPublic) {
    jdbcTemplate.update(
        """
        update albums
        set title = ?, slug = ?, description = ?, cover_url = ?, sort_order = ?, is_public = ?, updated_at = current_timestamp
        where id = ?
        """,
        title,
        slug,
        description,
        coverUrl,
        sortOrder,
        isPublic,
        id);
  }

  public void deleteAlbum(long id) {
    jdbcTemplate.update("delete from albums where id = ?", id);
  }

  public List<Map<String, Object>> listPhotoRows(long albumId, boolean publicOnly) {
    String where = publicOnly ? " where album_id = ? and is_public = true " : " where album_id = ? ";
    return jdbcTemplate.queryForList(
        "select * from album_photos" + where + " order by sort_order asc, created_at asc, id asc", albumId);
  }

  public Map<String, Object> findPhoto(long albumId, long photoId) {
    List<Map<String, Object>> rows =
        jdbcTemplate.queryForList("select * from album_photos where album_id = ? and id = ?", albumId, photoId);
    return rows.isEmpty() ? null : rows.getFirst();
  }

  public long createPhoto(
      long albumId,
      Long fileId,
      String imageUrl,
      String title,
      String description,
      int sortOrder,
      boolean isPublic,
      Instant takenAt) {
    return insertAndReturnId(
        """
        insert into album_photos (album_id, file_id, image_url, title, description, sort_order, is_public, taken_at)
        values (?, ?, ?, ?, ?, ?, ?, ?)
        """,
        albumId,
        fileId,
        imageUrl,
        title,
        description,
        sortOrder,
        isPublic,
        timestamp(takenAt));
  }

  public void updatePhoto(
      long albumId,
      long photoId,
      Long fileId,
      String imageUrl,
      String title,
      String description,
      int sortOrder,
      boolean isPublic,
      Instant takenAt) {
    jdbcTemplate.update(
        """
        update album_photos
        set file_id = ?, image_url = ?, title = ?, description = ?, sort_order = ?, is_public = ?, taken_at = ?, updated_at = current_timestamp
        where id = ? and album_id = ?
        """,
        fileId,
        imageUrl,
        title,
        description,
        sortOrder,
        isPublic,
        timestamp(takenAt),
        photoId,
        albumId);
  }

  public void deletePhoto(long albumId, long photoId) {
    jdbcTemplate.update("delete from album_photos where id = ? and album_id = ?", photoId, albumId);
  }

  public void reorderPhotos(long albumId, List<Long> photoIds) {
    int order = 1;
    for (Long photoId : photoIds) {
      jdbcTemplate.update(
          "update album_photos set sort_order = ?, updated_at = current_timestamp where album_id = ? and id = ?",
          order++,
          albumId,
          photoId);
    }
  }

  private Timestamp timestamp(Instant value) {
    return value == null ? null : Timestamp.from(value);
  }

  @Transactional
  long insertAndReturnId(String sql, Object... args) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
          for (int i = 0; i < args.length; i++) {
            statement.setObject(i + 1, args[i]);
          }
          return statement;
        },
        keyHolder);
    Map<String, Object> keys = keyHolder.getKeys();
    if (keys != null && keys.get("id") instanceof Number number) {
      return number.longValue();
    }
    return keyHolder.getKey().longValue();
  }
}
