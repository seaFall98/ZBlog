import { describe, expect, it } from "vitest";
import { normalizeMediaUrl } from "./mediaUrl";

describe("normalizeMediaUrl", () => {
  it("keeps remote http and https media URLs unchanged", () => {
    expect(normalizeMediaUrl("https://cdn.example.com/photo.jpg?size=large")).toBe("https://cdn.example.com/photo.jpg?size=large");
    expect(normalizeMediaUrl("http://cdn.example.com/photo.jpg")).toBe("http://cdn.example.com/photo.jpg");
  });

  it("keeps root-relative media URLs root-relative", () => {
    expect(normalizeMediaUrl("/uploads/albums/autumn.jpg")).toBe("/uploads/albums/autumn.jpg");
  });

  it("converts relative media URLs into root-relative URLs", () => {
    expect(normalizeMediaUrl("uploads/albums/autumn.jpg")).toBe("/uploads/albums/autumn.jpg");
    expect(normalizeMediaUrl("  media/photo one.jpg  ")).toBe("/media/photo one.jpg");
  });

  it("returns an empty string for missing media URLs", () => {
    expect(normalizeMediaUrl(null)).toBe("");
    expect(normalizeMediaUrl(undefined)).toBe("");
    expect(normalizeMediaUrl("   ")).toBe("");
  });
});
