export function getNextPhotoIndex(selectedIndex: number | null, photoCount: number): number | null {
  if (selectedIndex === null || photoCount <= 0) return null;
  return (selectedIndex + 1) % photoCount;
}

export function getPreviousPhotoIndex(selectedIndex: number | null, photoCount: number): number | null {
  if (selectedIndex === null || photoCount <= 0) return null;
  return (selectedIndex - 1 + photoCount) % photoCount;
}
