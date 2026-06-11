import { useCallback, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeftIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";
import GalleryPhotoModal from "../features/gallery/GalleryPhotoModal";
import SpatialGallery from "../features/gallery/SpatialGallery";
import { getNextPhotoIndex, getPreviousPhotoIndex } from "../features/gallery/galleryLayout";
import { useAlbum } from "../features/gallery/useAlbum";
import { toDateText } from "../lib/text";

export default function GalleryDetail() {
  const { slug } = useParams();
  const { album, loading } = useAlbum(slug ?? "");
  const photos = album?.photos ?? [];
  const [selectedIndex, setSelectedIndex] = useState<number | null>(null);

  const selectedPhoto = selectedIndex !== null ? (photos[selectedIndex] ?? null) : null;
  const albumTitle = album?.title ?? "相册";

  const closePhoto = useCallback(() => setSelectedIndex(null), []);

  const previousPhoto = useCallback(() => {
    setSelectedIndex((current) => getPreviousPhotoIndex(current, photos.length));
  }, [photos.length]);

  const nextPhoto = useCallback(() => {
    setSelectedIndex((current) => getNextPhotoIndex(current, photos.length));
  }, [photos.length]);

  return (
    <PageLayout>
      <div className="gallery-detail-page max-w-7xl mx-auto px-6 md:px-8 pt-12 pb-24">
        <Link
          to="/gallery"
          className="inline-flex items-center gap-1.5 text-sm mb-10 hover:opacity-60 transition-opacity"
          style={{ color: "var(--muted-ink)" }}
        >
          <ArrowLeftIcon size={14} aria-hidden="true" /> 返回相册
        </Link>

        <header className="gallery-detail-page__header mb-12">
          <p className="text-xs tracking-widest uppercase mb-3" style={{ color: "var(--muted-ink)" }}>
            {album ? toDateText(album.createdAt).slice(0, 7) : "Gallery"}
          </p>
          <h1 style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(30px,3.5vw,48px)", fontWeight: 400, color: "var(--ink)" }}>
            {album?.title ?? (loading ? "正在加载相册" : "相册不存在")}
          </h1>
          <p className="mt-3 text-sm" style={{ color: "var(--muted-ink)" }}>
            {album ? `${album.description} / ${photos.length} 张` : ""}
          </p>
        </header>

        <SpatialGallery photos={photos} albumTitle={albumTitle} onSelectPhoto={setSelectedIndex} />
      </div>

      <GalleryPhotoModal
        photo={selectedPhoto}
        albumTitle={albumTitle}
        photoCount={photos.length}
        selectedIndex={selectedIndex}
        onClose={closePhoto}
        onNext={nextPhoto}
        onPrevious={previousPhoto}
      />
    </PageLayout>
  );
}
