import { Link } from "react-router-dom";
import { ImageIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";
import { albums } from "../data/mockData";

export default function Gallery() {
  return (
    <PageLayout>
      <div className="max-w-7xl mx-auto px-8 pt-16 pb-24">
        {/* Header */}
        <div className="mb-14">
          <p className="text-xs tracking-widest uppercase mb-3" style={{ color: "var(--muted-ink)" }}>Gallery</p>
          <h1 style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(36px,4vw,56px)", fontWeight: 400, color: "var(--ink)" }}>
            相册
          </h1>
          <p className="mt-4 text-sm" style={{ color: "var(--muted-ink)" }}>{albums.length} 个相册</p>
        </div>

        {/* Albums masonry-like layout */}
        <div className="flex flex-wrap gap-4">
          {albums.map((album, idx) => {
            const isLarge = idx % 5 === 0 || idx % 5 === 3;
            return (
              <Link
                key={album.id}
                to={`/gallery/${album.id}`}
                className="group block overflow-hidden hover:-translate-y-1 transition-transform duration-300"
                style={{
                  flexBasis: isLarge ? "calc(58% - 8px)" : "calc(40% - 8px)",
                  minWidth: "240px",
                  border: "1px solid var(--warm-border)",
                }}
              >
                {/* Cover */}
                <div
                  className="overflow-hidden"
                  style={{ height: isLarge ? "320px" : "240px" }}
                >
                  <img
                    src={album.coverImage}
                    alt={album.title}
                    loading="lazy"
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                  />
                </div>

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
                  <div className="mt-3 text-xs" style={{ color: "var(--muted-ink)" }}>{album.date}</div>
                </div>
              </Link>
            );
          })}
        </div>
      </div>
    </PageLayout>
  );
}
