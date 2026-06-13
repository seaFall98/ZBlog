import { describe, expect, it } from "vitest";
import { getNextPhotoIndex, getPreviousPhotoIndex } from "./galleryLayout";

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
