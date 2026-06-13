import { useState, type CSSProperties } from "react";
import { ImageIcon } from "lucide-react";
import type { AlbumPhotoView } from "./types";

type GalleryMasonryProps = {
  photos: AlbumPhotoView[];
  albumTitle: string;
  loading: boolean;
  onSelectPhoto: (index: number) => void;
};

type MasonryPhotoProps = {
  photo: AlbumPhotoView;
  index: number;
  albumTitle: string;
  onSelectPhoto: (index: number) => void;
};

const masonryRatios = ["4 / 5", "3 / 4", "5 / 4", "1 / 1", "4 / 3", "2 / 3"] as const;
const masonryOffsets = ["0px", "18px", "6px", "28px", "10px", "22px"] as const;

function MasonryPhoto({ photo, index, albumTitle, onSelectPhoto }: MasonryPhotoProps) {
  const [loaded, setLoaded] = useState(false);
  const title = photo.title || albumTitle;
  const ratio = masonryRatios[index % masonryRatios.length];
  const offset = masonryOffsets[index % masonryOffsets.length];

  return (
    <button
      type="button"
      className={`gallery-masonry__item${loaded ? " gallery-masonry__item--loaded" : ""}`}
      style={{
        "--gallery-photo-ratio": ratio,
        "--gallery-photo-offset": offset,
      } as CSSProperties}
      onClick={() => onSelectPhoto(index)}
      aria-label={`查看照片：${title}`}
    >
      <span className="gallery-masonry__image-shell">
        <img
          src={photo.imageUrl}
          alt={title}
          loading={index < 3 ? "eager" : "lazy"}
          onLoad={() => setLoaded(true)}
        />
      </span>
      {(photo.title || photo.takenAt) && (
        <span className="gallery-masonry__caption">
          {photo.title && <span>{photo.title}</span>}
          {photo.takenAt && <small>{photo.takenAt.slice(0, 10)}</small>}
        </span>
      )}
    </button>
  );
}

function MasonrySkeleton() {
  return (
    <div className="gallery-masonry" aria-hidden="true">
      {Array.from({ length: 8 }, (_, index) => (
        <div
          key={index}
          className="gallery-masonry__item gallery-masonry__item--skeleton"
          style={{
            "--gallery-photo-ratio": masonryRatios[index % masonryRatios.length],
            "--gallery-photo-offset": masonryOffsets[index % masonryOffsets.length],
          } as CSSProperties}
        >
          <span className="gallery-masonry__image-shell" />
        </div>
      ))}
    </div>
  );
}

export default function GalleryMasonry({ photos, albumTitle, loading, onSelectPhoto }: GalleryMasonryProps) {
  if (loading) return <MasonrySkeleton />;

  if (photos.length === 0) {
    return (
      <div className="gallery-masonry gallery-masonry--empty">
        <ImageIcon size={28} aria-hidden="true" />
        <p>这个相册还没有照片。</p>
      </div>
    );
  }

  return (
    <section className="gallery-masonry" aria-label={`${albumTitle} 照片`}>
      {photos.map((photo, index) => (
        <MasonryPhoto
          key={photo.id}
          photo={photo}
          index={index}
          albumTitle={albumTitle}
          onSelectPhoto={onSelectPhoto}
        />
      ))}
    </section>
  );
}
