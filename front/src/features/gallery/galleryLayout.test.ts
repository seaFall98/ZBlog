import { describe, expect, it } from "vitest";
import { getNextPhotoIndex, getPreviousPhotoIndex, getSpatialPhotoLayout } from "./galleryLayout";

describe("getSpatialPhotoLayout", () => {
  it("creates a deterministic spatial layout for each photo", () => {
    const layout = getSpatialPhotoLayout(6);

    expect(layout).toHaveLength(6);
    expect(layout.map((item) => item.index)).toEqual([0, 1, 2, 3, 4, 5]);
    expect(layout.map((item) => item.variant)).toEqual(["hero", "portrait", "landscape", "small", "portrait", "landscape"]);
    expect(layout[0]).toMatchObject({ index: 0, x: 42, y: 12, rotate: -2, zIndex: 20 });
    expect(layout[3]).toMatchObject({ index: 3, x: 24, y: 54, rotate: -5 });
  });

  it("returns no positions when there are no photos", () => {
    expect(getSpatialPhotoLayout(0)).toEqual([]);
    expect(getSpatialPhotoLayout(-2)).toEqual([]);
  });
});

describe("photo index navigation", () => {
  it("wraps to next and previous photo indexes", () => {
    expect(getNextPhotoIndex(0, 3)).toBe(1);
    expect(getNextPhotoIndex(2, 3)).toBe(0);
    expect(getPreviousPhotoIndex(2, 3)).toBe(1);
    expect(getPreviousPhotoIndex(0, 3)).toBe(2);
  });

  it("returns null when navigation cannot resolve a selected photo", () => {
    expect(getNextPhotoIndex(null, 3)).toBeNull();
    expect(getPreviousPhotoIndex(null, 3)).toBeNull();
    expect(getNextPhotoIndex(0, 0)).toBeNull();
    expect(getPreviousPhotoIndex(0, 0)).toBeNull();
  });
});
