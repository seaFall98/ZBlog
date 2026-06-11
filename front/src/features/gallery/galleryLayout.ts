export type SpatialPhotoVariant = "hero" | "portrait" | "landscape" | "small";

export type SpatialPhotoLayoutItem = {
  index: number;
  variant: SpatialPhotoVariant;
  x: number;
  y: number;
  rotate: number;
  rotateY: number;
  depth: number;
  scale: number;
  zIndex: number;
};

type SpatialPatternItem = Omit<SpatialPhotoLayoutItem, "index">;

const HERO_SLOT: SpatialPatternItem = {
  variant: "hero",
  x: 50,
  y: 43,
  rotate: -1.5,
  rotateY: 0,
  depth: 46,
  scale: 1,
  zIndex: 40,
};

const ORBITAL_SLOTS: SpatialPatternItem[] = [
  { variant: "portrait", x: 15, y: 16, rotate: 5.5, rotateY: -7, depth: 10, scale: 0.98, zIndex: 17 },
  { variant: "small", x: 35, y: 10, rotate: -3, rotateY: 4, depth: 4, scale: 0.86, zIndex: 12 },
  { variant: "landscape", x: 67, y: 15, rotate: 3.5, rotateY: -5, depth: 6, scale: 0.92, zIndex: 14 },
  { variant: "portrait", x: 84, y: 15, rotate: -4.5, rotateY: 7, depth: 2, scale: 0.95, zIndex: 13 },
  { variant: "portrait", x: 4, y: 45, rotate: -3.5, rotateY: -5, depth: -2, scale: 0.9, zIndex: 10 },
  { variant: "small", x: 29, y: 48, rotate: 2.5, rotateY: 4, depth: 8, scale: 0.9, zIndex: 18 },
  { variant: "portrait", x: 73, y: 43, rotate: 4.5, rotateY: -4, depth: 12, scale: 0.96, zIndex: 16 },
  { variant: "landscape", x: 17, y: 73, rotate: -2.5, rotateY: 6, depth: 0, scale: 0.88, zIndex: 11 },
  { variant: "small", x: 40, y: 79, rotate: 2, rotateY: -3, depth: 5, scale: 0.82, zIndex: 15 },
  { variant: "portrait", x: 61, y: 74, rotate: -4, rotateY: 5, depth: 7, scale: 0.92, zIndex: 13 },
  { variant: "small", x: 95, y: 70, rotate: 6, rotateY: -6, depth: -4, scale: 0.84, zIndex: 9 },
];

const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value));

function withSafeCycleJitter(slot: SpatialPatternItem, slotIndex: number, cycle: number): SpatialPatternItem {
  if (cycle === 0) return slot;

  const xJitter = ((cycle * 5 + slotIndex * 3) % 7) - 3;
  const yJitter = ((cycle * 4 + slotIndex * 2) % 7) - 3;
  const rotateJitter = (((cycle + slotIndex) % 5) - 2) * 0.8;
  const depthJitter = ((cycle + slotIndex) % 3) * 2;

  return {
    ...slot,
    x: clamp(slot.x + xJitter, 4, 96),
    y: clamp(slot.y + yJitter, 9, 82),
    rotate: Number((slot.rotate + rotateJitter).toFixed(1)),
    rotateY: Number((slot.rotateY - rotateJitter).toFixed(1)),
    depth: slot.depth + depthJitter,
    scale: Number(Math.max(0.78, slot.scale - cycle * 0.02).toFixed(2)),
    zIndex: Math.max(2, slot.zIndex - cycle * 2),
  };
}

export function getSpatialPhotoLayout(count: number): SpatialPhotoLayoutItem[] {
  if (count <= 0) return [];

  return Array.from({ length: count }, (_, index) => {
    if (index === 0) {
      return { index, ...HERO_SLOT };
    }

    const orbitalIndex = (index - 1) % ORBITAL_SLOTS.length;
    const cycle = Math.floor((index - 1) / ORBITAL_SLOTS.length);
    const slot = withSafeCycleJitter(ORBITAL_SLOTS[orbitalIndex], orbitalIndex, cycle);

    return { index, ...slot };
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
