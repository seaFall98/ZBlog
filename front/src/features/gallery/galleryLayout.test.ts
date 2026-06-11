import { describe, expect, it } from "vitest";
import { getNextPhotoIndex, getPreviousPhotoIndex, getSpatialPhotoLayout } from "./galleryLayout";

describe("getSpatialPhotoLayout", () => {
  it("uses the first photo as the central hero image", () => {
    const layout = getSpatialPhotoLayout(1);

    expect(layout).toHaveLength(1);
    expect(layout[0]).toMatchObject({
      index: 0,
      variant: "hero",
      x: 50,
      y: 43,
      zIndex: 40,
    });
  });

  it("places eight photos around a controlled white-space stage", () => {
    const layout = getSpatialPhotoLayout(8);

    expect(layout).toHaveLength(8);
    expect(layout.map((item) => item.index)).toEqual([0, 1, 2, 3, 4, 5, 6, 7]);
    expect(layout.map((item) => item.variant)).toEqual([
      "hero",
      "portrait",
      "small",
      "landscape",
      "portrait",
      "portrait",
      "small",
      "portrait",
    ]);
    expect(layout[1]).toMatchObject({ x: 15, y: 16, rotate: 5.5 });
    expect(layout[7]).toMatchObject({ x: 73, y: 43, rotate: 4.5 });
  });

  it("keeps the first twelve photos within the spatial canvas", () => {
    const layout = getSpatialPhotoLayout(12);

    expect(layout).toHaveLength(12);
    expect(layout[0].zIndex).toBeGreaterThan(layout[1].zIndex);
    expect(layout.slice(1).some((item) => item.x <= 5 || item.x >= 95)).toBe(true);
    layout.forEach((item) => {
      expect(item.x).toBeGreaterThanOrEqual(4);
      expect(item.x).toBeLessThanOrEqual(96);
      expect(item.y).toBeGreaterThanOrEqual(9);
      expect(item.y).toBeLessThanOrEqual(82);
    });
  });

  it("cycles extra photos with safe jitter instead of drifting away", () => {
    const layout = getSpatialPhotoLayout(20);

    expect(layout).toHaveLength(20);
    expect(layout[12].variant).toBe(layout[1].variant);
    expect(layout[12].x).not.toBe(layout[1].x);
    layout.forEach((item) => {
      expect(item.x).toBeGreaterThanOrEqual(4);
      expect(item.x).toBeLessThanOrEqual(96);
      expect(item.y).toBeGreaterThanOrEqual(9);
      expect(item.y).toBeLessThanOrEqual(82);
      expect(item.scale).toBeGreaterThanOrEqual(0.78);
    });
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
