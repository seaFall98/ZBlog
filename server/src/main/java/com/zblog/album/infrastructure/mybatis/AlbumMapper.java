package com.zblog.album.infrastructure.mybatis;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AlbumMapper {

  long countPublicAlbums();

  List<Map<String, Object>> listPublicRows(@Param("limit") int limit, @Param("offset") int offset);

  List<Map<String, Object>> publicRowsBySlug(@Param("slug") String slug);

  long countAdminRows(@Param("keyword") String keyword, @Param("isPublic") Boolean isPublic);

  List<Map<String, Object>> listAdminRows(
      @Param("keyword") String keyword,
      @Param("isPublic") Boolean isPublic,
      @Param("limit") int limit,
      @Param("offset") int offset);

  List<Map<String, Object>> albumById(@Param("id") long id);

  void insertAlbum(Map<String, Object> params);

  void updateAlbum(Map<String, Object> params);

  void deleteAlbum(@Param("id") long id);

  List<Map<String, Object>> listPhotoRows(
      @Param("albumId") long albumId, @Param("publicOnly") boolean publicOnly);

  List<Map<String, Object>> photoById(@Param("albumId") long albumId, @Param("photoId") long photoId);

  void insertPhoto(Map<String, Object> params);

  void updatePhoto(Map<String, Object> params);

  void deletePhoto(@Param("albumId") long albumId, @Param("photoId") long photoId);

  void updatePhotoSort(
      @Param("albumId") long albumId, @Param("photoId") long photoId, @Param("sortOrder") int sortOrder);
}
