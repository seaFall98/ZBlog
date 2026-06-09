export type SpatialPhotoVariant = "hero" | "portrait" | "landscape" | "small";

export type SpatialPhotoLayoutItem = {
  index: number;
  variant: SpatialPhotoVariant;
  x: number;
  y: number;
  rotate: number;
  zIndex: number;
};

const SPATIAL_PATTERN: Omit<SpatialPhotoLayoutItem, "index">[] = [
  { variant: "hero", x: 42, y: 12, rotate: -2, zIndex: 20 },
  { variant: "portrait", x: 10, y: 8, rotate: 4, zIndex: 12 },
  { variant: "landscape", x: 66, y: 36, rotate: 3, zIndex: 14 },
  { variant: "small", x: 24, y: 54, rotate: -5, zIndex: 10 },
  { variant: "portrait", x: 78, y: 4, rotate: -4, zIndex: 11 },
  { variant: "landscape", x: 6, y: 34, rotate: 2, zIndex: 9 },
  { variant: "small", x: 55, y: 61, rotate: 5, zIndex: 8 },
  { variant: "portrait", x: 86, y: 58, rotate: -2, zIndex: 7 },
];

export function getSpatialPhotoLayout(count: number): SpatialPhotoLayoutItem[] {
  if (count <= 0) return [];

  return Array.from({ length: count }, (_, index) => {
    const pattern = SPATIAL_PATTERN[index % SPATIAL_PATTERN.length];
    const cycle = Math.floor(index / SPATIAL_PATTERN.length);
    return {
      index,
      variant: pattern.variant,
      x: (pattern.x + cycle * 9) % 88,
      y: pattern.y + cycle * 18,
      rotate: pattern.rotate,
      zIndex: Math.max(1, pattern.zIndex - cycle),
    };
  });
}

export function getNextPhotoIndex(selectedIndex: number | null, photoCount: number): number | null {
  if (selectedIndex === null || photoCount <= 0) return null;
  return (selectedIndex + 1) % photoCount;
}

export function getPreviousPhotoIndex(selectedIndex: number | null, photoCount: number): number | null {
  if (selectedIndex === null || photoCount <= 0) return null;
  return (selectedIndex - 1 + photoCount) % photoCount;
}
