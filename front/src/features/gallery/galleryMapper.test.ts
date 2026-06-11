import { describe, expect, it } from "vitest";
import { mapAlbum, mapAlbumList } from "./galleryMapper";

describe("galleryMapper", () => {
  it("normalizes album cover and photo image URLs", () => {
    const album = mapAlbum({
      id: 12,
      slug: "autumn-room",
      title: "秋日房间",
      cover_url: "uploads/albums/cover.jpg",
      photos: [
        { id: 1, album_id: 12, image_url: "media/photo-a.jpg", title: "Photo A" },
        { id: 2, album_id: 12, image_url: "/media/photo-b.jpg", title: "Photo B" },
        { id: 3, album_id: 12, image_url: "https://cdn.example.com/photo-c.jpg", title: "Photo C" },
      ],
    });

    expect(album?.coverUrl).toBe("/uploads/albums/cover.jpg");
    expect(album?.photos.map((photo) => photo.imageUrl)).toEqual([
      "/media/photo-a.jpg",
      "/media/photo-b.jpg",
      "https://cdn.example.com/photo-c.jpg",
    ]);
  });

  it("normalizes album covers in list responses", () => {
    const result = mapAlbumList({
      list: [{ id: 1, slug: "daily", title: "日常", cover_url: "covers/daily.jpg" }],
      total: 1,
      page: 1,
      page_size: 20,
    });

    expect(result.albums[0]?.coverUrl).toBe("/covers/daily.jpg");
  });
});
