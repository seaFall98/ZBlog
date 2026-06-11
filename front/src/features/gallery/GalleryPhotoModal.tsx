import { useEffect, useRef } from "react";
import { createPortal } from "react-dom";
import { ChevronLeftIcon, ChevronRightIcon, XIcon } from "lucide-react";
import { toDateText } from "../../lib/text";
import type { AlbumPhotoView } from "./types";

type GalleryPhotoModalProps = {
  photo: AlbumPhotoView | null;
  albumTitle: string;
  photoCount: number;
  selectedIndex: number | null;
  onClose: () => void;
  onNext: () => void;
  onPrevious: () => void;
};

export default function GalleryPhotoModal({
  photo,
  albumTitle,
  photoCount,
  selectedIndex,
  onClose,
  onNext,
  onPrevious,
}: GalleryPhotoModalProps) {
  const previousBodyOverflow = useRef<string | null>(null);
  const isOpen = Boolean(photo);
  const displayTitle = photo?.title ?? "";
  const displayDate = photo?.takenAt ? toDateText(photo.takenAt) : "";

  useEffect(() => {
    if (!isOpen || typeof document === "undefined") return;

    if (previousBodyOverflow.current === null) {
      previousBodyOverflow.current = document.body.style.overflow;
    }
    document.body.style.overflow = "hidden";

    return () => {
      if (previousBodyOverflow.current !== null) {
        document.body.style.overflow = previousBodyOverflow.current;
        previousBodyOverflow.current = null;
      }
    };
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen || typeof window === "undefined") return;

    const handleKeydown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
      if (event.key === "ArrowRight") onNext();
      if (event.key === "ArrowLeft") onPrevious();
    };

    window.addEventListener("keydown", handleKeydown);
    return () => window.removeEventListener("keydown", handleKeydown);
  }, [isOpen, onClose, onNext, onPrevious]);

  if (!photo || typeof document === "undefined") return null;

  return createPortal(
    <div className="gallery-photo-modal" role="dialog" aria-modal="true" aria-label={displayTitle} onClick={onClose}>
      <img className="gallery-photo-modal__backdrop-image" src={photo.imageUrl} alt="" aria-hidden="true" />
      <button className="gallery-photo-modal__close" type="button" aria-label="关闭" onClick={onClose}>
        <XIcon size={20} aria-hidden="true" />
      </button>
      <button className="gallery-photo-modal__nav gallery-photo-modal__nav--prev" type="button" aria-label="上一张" onClick={(event) => { event.stopPropagation(); onPrevious(); }}>
        <ChevronLeftIcon size={24} aria-hidden="true" />
      </button>
      <figure className="gallery-photo-modal__stage" onClick={(event) => event.stopPropagation()}>
        <div className="gallery-photo-modal__image-wrap">
          <img src={photo.imageUrl} alt={displayTitle || albumTitle} />
        </div>
        <figcaption className="gallery-photo-modal__panel">
          <p className="gallery-photo-modal__album">{albumTitle}</p>
          <h2>{displayTitle}</h2>
          {photo.description && <p className="gallery-photo-modal__description">{photo.description}</p>}
          <div className="gallery-photo-modal__meta" aria-label="照片信息">
            <div>
              <span>位置</span>
              <strong>{selectedIndex !== null ? selectedIndex + 1 : 0} / {photoCount}</strong>
            </div>
            {displayDate && (
              <div>
                <span>时间</span>
                <strong>{displayDate}</strong>
              </div>
            )}
          </div>
        </figcaption>
      </figure>
      <button className="gallery-photo-modal__nav gallery-photo-modal__nav--next" type="button" aria-label="下一张" onClick={(event) => { event.stopPropagation(); onNext(); }}>
        <ChevronRightIcon size={24} aria-hidden="true" />
      </button>
    </div>,
    document.body,
  );
}
