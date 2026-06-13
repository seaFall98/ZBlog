import { useCallback, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeftIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";
import GalleryMasonry from "../features/gallery/GalleryMasonry";
import GalleryPhotoModal from "../features/gallery/GalleryPhotoModal";
import { getNextPhotoIndex, getPreviousPhotoIndex } from "../features/gallery/galleryLayout";
import { useAlbum } from "../features/gallery/useAlbum";
import { toDateText } from "../lib/text";

export default function GalleryDetail() {
  const { slug } = useParams();
  const { album, loading, error } = useAlbum(slug ?? "");
  const photos = album?.photos ?? [];
  const [selectedIndex, setSelectedIndex] = useState<number | null>(null);

  const selectedPhoto = selectedIndex !== null ? (photos[selectedIndex] ?? null) : null;
  const albumTitle = album?.title ?? "相册";
  const photoCount = album?.photoCount || photos.length;
  const createdAtText = album?.createdAt ? toDateText(album.createdAt) : "";

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

        <header className="gallery-detail-page__header">
          <div className="gallery-detail-page__intro">
            <p className="text-xs tracking-widest uppercase mb-3" style={{ color: "var(--muted-ink)" }}>
              Gallery
            </p>
            <h1>{album?.title ?? (loading ? "正在加载相册" : "相册不存在")}</h1>
            {album?.description && <p className="gallery-detail-page__description">{album.description}</p>}
          </div>

          <dl className="gallery-detail-page__meta" aria-label="相册信息">
            <div>
              <dt>时间</dt>
              <dd>{createdAtText || "未发布"}</dd>
            </div>
            <div>
              <dt>照片</dt>
              <dd>{photoCount} 张</dd>
            </div>
          </dl>
        </header>

        {error ? (
          <div className="gallery-masonry gallery-masonry--empty" role="status">
            相册加载失败，请稍后再试。
          </div>
        ) : (
          <GalleryMasonry
            photos={photos}
            albumTitle={albumTitle}
            loading={loading}
            onSelectPhoto={setSelectedIndex}
          />
        )}
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
