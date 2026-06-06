import { useState } from "react";
import { Link } from "react-router-dom";
import { ArrowLeftIcon, XIcon, ChevronLeftIcon, ChevronRightIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";
import { albums, galleryPhotos } from "../data/mockData";

const DEMO_ALBUM = albums[0];

export default function GalleryDetail() {
  const album = DEMO_ALBUM;
  const photos = galleryPhotos.filter((p) => p.albumId === album.id);
  const [selectedIndex, setSelectedIndex] = useState<number | null>(null);

  const selectedPhoto = selectedIndex !== null ? (photos[selectedIndex] ?? null) : null;

  const prev = () => {
    if (selectedIndex === null || photos.length === 0) return;
    setSelectedIndex((selectedIndex - 1 + photos.length) % photos.length);
  };

  const next = () => {
    if (selectedIndex === null || photos.length === 0) return;
    setSelectedIndex((selectedIndex + 1) % photos.length);
  };

  return (
    <PageLayout>
      <div className="max-w-7xl mx-auto px-8 pt-12 pb-24">
        {/* Back */}
        <Link
          to="/gallery"
          className="inline-flex items-center gap-1.5 text-sm mb-10 hover:opacity-60 transition-opacity"
          style={{ color: "var(--muted-ink)" }}
        >
          <ArrowLeftIcon size={14} /> 返回相册
        </Link>

        {/* Album header */}
        <div className="mb-12">
          <p className="text-xs tracking-widest uppercase mb-3" style={{ color: "var(--muted-ink)" }}>{album.date}</p>
          <h1 style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(30px,3.5vw,48px)", fontWeight: 400, color: "var(--ink)" }}>
            {album.title}
          </h1>
          <p className="mt-3 text-sm" style={{ color: "var(--muted-ink)" }}>{album.description} · {photos.length} 张</p>
        </div>

        {/* Photo grid */}
        <div className="flex flex-wrap gap-2">
          {photos.map((photo, idx) => {
            const spans = [2, 1, 1, 2, 1, 2, 1, 1];
            const isWide = (spans[idx % spans.length] ?? 1) === 2;
            return (
              <button
                key={photo.id}
                onClick={() => setSelectedIndex(idx)}
                className="group overflow-hidden gallery-photo cursor-pointer"
                style={{
                  flexBasis: isWide ? "calc(55% - 4px)" : "calc(43% - 4px)",
                  minWidth: "200px",
                  height: isWide ? "280px" : "200px",
                  border: "none",
                  padding: 0,
                }}
              >
                <img
                  src={photo.src}
                  alt={photo.title}
                  loading="lazy"
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                />
              </button>
            );
          })}
        </div>
      </div>

      {/* ── Magazine Photo Modal ── */}
      <div
        className="fixed inset-0 z-50 flex"
        style={{
          visibility: selectedPhoto ? "visible" : "hidden",
          opacity: selectedPhoto ? 1 : 0,
          transition: "opacity 0.3s ease",
          pointerEvents: selectedPhoto ? "auto" : "none",
        }}
      >
        {/* Left: white info panel */}
        <div
          className="flex flex-col justify-between p-10 shrink-0"
          style={{
            width: "320px",
            background: "#FFFFFF",
            borderRight: "1px solid var(--warm-border)",
          }}
        >
          <div>
            <button
              onClick={() => setSelectedIndex(null)}
              className="mb-10 flex items-center gap-1.5 text-xs hover:opacity-60 transition-opacity"
              style={{ color: "var(--muted-ink)" }}
            >
              <XIcon size={13} /> 关闭
            </button>
            <div className="text-xs tracking-widest uppercase mb-4" style={{ color: "var(--muted-ink)" }}>
              {album.title}
            </div>
            <h2
              className="mb-4"
              style={{ fontFamily: "var(--fontDisplay)", fontSize: "22px", fontWeight: 400, color: "var(--ink)", lineHeight: 1.35 }}
            >
              {selectedPhoto?.title ?? ""}
            </h2>
            <p className="text-sm leading-relaxed" style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>
              {selectedPhoto?.description ?? ""}
            </p>
          </div>

          <div>
            <div className="text-xs mb-1" style={{ color: "var(--muted-ink)" }}>{selectedPhoto?.date ?? ""}</div>
            <div className="text-xs" style={{ color: "var(--muted-ink)" }}>{selectedPhoto?.filename ?? ""}</div>
            {/* Nav */}
            <div className="flex gap-2 mt-6">
              <button
                onClick={prev}
                className="flex items-center justify-center w-10 h-10 border hover:border-primary transition-colors"
                style={{ borderColor: "var(--warm-border)" }}
              >
                <ChevronLeftIcon size={16} style={{ color: "var(--ink)" }} />
              </button>
              <button
                onClick={next}
                className="flex items-center justify-center w-10 h-10 border hover:border-primary transition-colors"
                style={{ borderColor: "var(--warm-border)" }}
              >
                <ChevronRightIcon size={16} style={{ color: "var(--ink)" }} />
              </button>
              <span
                className="ml-auto flex items-center text-xs"
                style={{ color: "var(--muted-ink)" }}
              >
                {selectedIndex !== null ? selectedIndex + 1 : "—"} / {photos.length}
              </span>
            </div>
          </div>
        </div>

        {/* Right: black image panel */}
        <div
          className="flex-1 flex items-center justify-center"
          style={{ background: "#111111" }}
          onClick={() => setSelectedIndex(null)}
        >
          {selectedPhoto && (
            <img
              src={selectedPhoto.src.replace("w=800", "w=1200")}
              alt={selectedPhoto.title}
              className="max-h-screen max-w-full object-contain"
              style={{ maxWidth: "calc(100vw - 320px)", maxHeight: "100vh" }}
              onClick={(e) => e.stopPropagation()}
            />
          )}
        </div>
      </div>
    </PageLayout>
  );
}
