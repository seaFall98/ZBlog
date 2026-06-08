import { Link } from "react-router-dom";
import { ImageIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";
import { useAlbums } from "../features/gallery/useAlbums";
import { toDateText } from "../lib/text";

export default function Gallery() {
  const { albums, loading } = useAlbums(50);

  return (
    <PageLayout>
      <div className="max-w-7xl mx-auto px-8 pt-16 pb-24">
        {/* Header */}
        <div className="mb-14">
          <p className="text-xs tracking-widest uppercase mb-3" style={{ color: "var(--muted-ink)" }}>Gallery</p>
          <h1 style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(36px,4vw,56px)", fontWeight: 400, color: "var(--ink)" }}>
            相册
          </h1>
          <p className="mt-4 text-sm" style={{ color: "var(--muted-ink)" }}>{loading ? "正在加载相册..." : `${albums.length} 个相册`}</p>
        </div>

        {/* Albums masonry-like layout */}
        <div className="flex flex-wrap gap-4">
          {albums.map((album, idx) => {
            const isLarge = idx % 5 === 0 || idx % 5 === 3;
            return (
              <Link
                key={album.id}
                to={`/gallery/${album.slug}`}
                className="group block overflow-hidden hover:-translate-y-1 transition-transform duration-300"
                style={{
                  flexBasis: isLarge ? "calc(58% - 8px)" : "calc(40% - 8px)",
                  minWidth: "240px",
                  border: "1px solid var(--warm-border)",
                }}
              >
                {/* Cover */}
                {album.coverUrl && (
                  <div
                    className="overflow-hidden"
                    style={{ height: isLarge ? "320px" : "240px" }}
                  >
                    <img
                      src={album.coverUrl}
                      alt={album.title}
                      loading="lazy"
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    />
                  </div>
                )}

                {/* Info */}
                <div className="p-5" style={{ background: "var(--warm-white)" }}>
                  <div className="flex items-start justify-between">
                    <div>
                      <h2
                        className="mb-1"
                        style={{ fontFamily: "var(--fontDisplay)", fontSize: "18px", fontWeight: 500, color: "var(--ink)" }}
                      >
                        {album.title}
                      </h2>
                      <p className="text-sm" style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>
                        {album.description}
                      </p>
                    </div>
                    <div className="flex items-center gap-1 text-xs shrink-0 ml-3" style={{ color: "var(--muted-ink)" }}>
                      <ImageIcon size={12} />{album.photoCount}
                    </div>
                  </div>
                  <div className="mt-3 text-xs" style={{ color: "var(--muted-ink)" }}>{toDateText(album.createdAt).slice(0, 7)}</div>
                </div>
              </Link>
            );
          })}
        </div>

        {albums.length === 0 && (
          <div className="text-center py-20">
            <ImageIcon size={32} className="mx-auto mb-4" style={{ color: "var(--warm-border)" }} />
            <p style={{ color: "var(--muted-ink)" }}>{loading ? "正在翻阅相册..." : "相册暂时还是空白"}</p>
          </div>
        )}
      </div>
    </PageLayout>
  );
}
