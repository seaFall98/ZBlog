import type { AlbumPhotoView } from "./types";
import { getSpatialPhotoLayout } from "./galleryLayout";

type SpatialGalleryProps = {
  photos: AlbumPhotoView[];
  albumTitle: string;
  onSelectPhoto: (index: number) => void;
};

const variantClassName = {
  hero: "spatial-gallery__photo--hero",
  portrait: "spatial-gallery__photo--portrait",
  landscape: "spatial-gallery__photo--landscape",
  small: "spatial-gallery__photo--small",
} as const;

export default function SpatialGallery({ photos, albumTitle, onSelectPhoto }: SpatialGalleryProps) {
  const layout = getSpatialPhotoLayout(photos.length);

  if (photos.length === 0) {
    return (
      <div className="spatial-gallery spatial-gallery--empty">
        <p>这个空间还没有挂上照片。</p>
      </div>
    );
  }

  return (
    <section className="spatial-gallery" aria-label={`${albumTitle} 照片空间`}>
      {layout.map((item) => {
        const photo = photos[item.index];
        if (!photo) return null;

        return (
          <button
            key={photo.id}
            type="button"
            className={`spatial-gallery__photo ${variantClassName[item.variant]}`}
            style={{
              left: `${item.x}%`,
              top: `${item.y}%`,
              transform: `translate3d(-50%, -50%, ${item.depth}px) rotateY(${item.rotateY}deg) rotate(${item.rotate}deg) scale(${item.scale})`,
              zIndex: item.zIndex,
            }}
            onClick={() => onSelectPhoto(item.index)}
            aria-label={`查看照片：${photo.title || albumTitle}`}
          >
            <img src={photo.imageUrl} alt={photo.title || albumTitle} loading={item.index === 0 ? "eager" : "lazy"} />
          </button>
        );
      })}
    </section>
  );
}
