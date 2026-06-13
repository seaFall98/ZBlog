import PageLayout from "../components/layout/PageLayout";
import { AppPagination } from "../components/ui/app-pagination";
import { useMoments } from "../features/moments/useMoments";
import { useNormalizePage, usePage } from "../hooks/usePage";
import { toDateText } from "../lib/text";

const moodColors: Record<string, string> = {
  慵懒: "#B5956A",
  满足: "#7A8C6E",
  平静: "#7D8FA3",
  好奇: "#9B7FB6",
  感动: "#C0786A",
  空旷: "#8EA1A8",
  怀旧: "#A98569",
  专注: "#6E8A74",
};

const PAGE_SIZE = 10;

export default function Moments() {
  const { page, setPage } = usePage();
  const { moments, total, loading } = useMoments(page, PAGE_SIZE);
  const totalPages = Math.ceil(total / PAGE_SIZE);
  useNormalizePage(page, setPage, totalPages, loading);

  return (
    <PageLayout>
      <div className="max-w-5xl mx-auto px-8 pt-16 pb-24">
        <div className="mb-14">
          <p className="text-xs tracking-widest uppercase mb-3" style={{ color: "var(--muted-ink)" }}>Moments</p>
          <h1 style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(36px,4vw,56px)", fontWeight: 400, color: "var(--ink)" }}>
            生活瞬间
          </h1>
          <p className="mt-4 text-sm" style={{ color: "var(--muted-ink)" }}>{loading ? "正在加载生活瞬间..." : "记录生命中不想遗忘的碎片"}</p>
        </div>

        <div className="relative">
          <div className="absolute left-16 top-0 bottom-0 w-px hidden md:block" style={{ background: "var(--warm-border)" }} />

          <div className="flex flex-col gap-0">
            {moments.map((moment, idx) => {
              const moodColor = moodColors[moment.mood] ?? "var(--muted-ink)";
              return (
                <div key={moment.id} className="relative flex gap-8">
                  <div className="hidden md:flex flex-col items-center pt-6 shrink-0 w-32">
                    <div
                      className="w-2.5 h-2.5 rounded-full shrink-0 ring-2"
                      style={{ background: moodColor, outline: `3px solid var(--ivory)`, outlineOffset: "1px", zIndex: 1 }}
                    />
                    <div className="text-xs mt-2 text-center" style={{ color: "var(--muted-ink)" }}>
                      {toDateText(moment.date).slice(5)}
                    </div>
                  </div>

                  <div className="flex-1 mb-8 p-6" style={{ background: "var(--warm-white)", border: "1px solid var(--warm-border)" }}>
                    <div className="flex items-center gap-3 mb-3 md:hidden">
                      <span className="text-xs" style={{ color: "var(--muted-ink)" }}>{toDateText(moment.date)}</span>
                    </div>

                    <div className="flex items-center gap-2 mb-4">
                      <span className="w-2 h-2 rounded-full inline-block" style={{ background: moodColor }} />
                      <span className="text-xs" style={{ color: moodColor, fontFamily: "var(--fontSans)" }}>{moment.mood}</span>
                    </div>

                    <p className="leading-relaxed" style={{ fontFamily: "var(--fontBody)", fontSize: "15px", color: "var(--ink)", lineHeight: 1.95 }}>
                      {moment.text}
                    </p>

                    {moment.images.length > 0 && (
                      <div className="flex gap-2 mt-5 flex-wrap">
                        {moment.images.map((src) => (
                          <div key={src} className="overflow-hidden" style={{ width: moment.images.length === 1 ? "100%" : "calc(50% - 4px)", height: "180px" }}>
                            <img src={src} alt="" loading="lazy" className="w-full h-full object-cover" />
                          </div>
                        ))}
                      </div>
                    )}

                    {moment.video && (
                      <div className="mt-5 rounded-sm overflow-hidden" style={{ background: "#000" }}>
                        {moment.video.platform === "bilibili" && moment.video.videoId ? (
                          <iframe
                            src={`//player.bilibili.com/player.html?bvid=${moment.video.videoId}&autoplay=0`}
                            scrolling="no"
                            frameBorder="0"
                            allowFullScreen
                            className="w-full aspect-video border-0 block"
                          />
                        ) : moment.video.platform === "youtube" && moment.video.videoId ? (
                          <iframe
                            src={`https://www.youtube.com/embed/${moment.video.videoId}`}
                            frameBorder="0"
                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                            allowFullScreen
                            className="w-full aspect-video border-0 block"
                          />
                        ) : (
                          <video src={moment.video.url} controls preload="metadata" className="w-full aspect-video block" />
                        )}
                      </div>
                    )}

                    {moment.audio && (
                      <div className="mt-5">
                        <audio src={moment.audio} controls className="w-full" />
                      </div>
                    )}

                    {moment.music && (
                      <div className="mt-5 rounded-sm overflow-hidden" style={{ border: "1px solid var(--warm-border)", background: "var(--warm-white)" }}>
                        <div className="flex items-center gap-3 p-3">
                          {/* Cover */}
                          <div className="w-14 h-14 shrink-0 rounded-sm overflow-hidden" style={{ background: "var(--section-bg)" }}>
                            {moment.music.cover ? (
                              <img src={moment.music.cover} alt="" className="w-full h-full object-cover" />
                            ) : (
                              <div className="w-full h-full flex items-center justify-center" style={{ color: "var(--muted-ink)" }}>
                                ♪
                              </div>
                            )}
                          </div>
                          {/* Info + audio */}
                          <div className="flex-1 min-w-0">
                            <div className="text-sm truncate" style={{ color: "var(--ink)", fontFamily: "var(--fontSans)" }}>
                              {moment.music.title}
                            </div>
                            {moment.music.artist && (
                              <div className="text-xs mt-0.5" style={{ color: "var(--muted-ink)" }}>
                                {moment.music.artist}
                              </div>
                            )}
                            {moment.music.url ? (
                              <audio src={moment.music.url} controls className="w-full mt-2" style={{ height: "32px" }} />
                            ) : (
                              <a
                                href={moment.music.url}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="inline-block text-xs mt-2 underline"
                                style={{ color: "var(--muted-ink)" }}
                              >
                                前往收听 →
                              </a>
                            )}
                          </div>
                        </div>
                      </div>
                    )}

                    {moment.link && (
                      <a
                        href={moment.link.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center gap-3 mt-5 p-3 border rounded-sm transition-opacity hover:opacity-80"
                        style={{ borderColor: "var(--warm-border)", background: "var(--warm-white)" }}
                      >
                        <div className="w-10 h-10 shrink-0 flex items-center justify-center rounded-sm overflow-hidden" style={{ background: "var(--section-bg)" }}>
                          {moment.link.favicon ? (
                            <img src={moment.link.favicon} alt="" className="w-5 h-5 object-contain" onError={(e) => { (e.target as HTMLImageElement).style.display = "none"; }} />
                          ) : (
                            <span className="text-xs" style={{ color: "var(--muted-ink)" }}>🔗</span>
                          )}
                        </div>
                        <div className="min-w-0">
                          <div className="text-xs truncate" style={{ color: "var(--ink)", fontFamily: "var(--fontSans)" }}>
                            {moment.link.title}
                          </div>
                          <div className="text-xs truncate mt-0.5" style={{ color: "var(--muted-ink)" }}>
                            {moment.link.url}
                          </div>
                        </div>
                      </a>
                    )}

                    {moment.tags.length > 0 && (
                      <div className="flex flex-wrap gap-2 mt-4">
                        {moment.tags.map((tag) => (
                          <span key={tag} className="text-xs px-2 py-0.5" style={{ background: "var(--section-bg)", color: "var(--muted-ink)" }}>#{tag}</span>
                        ))}
                      </div>
                    )}

                    {moment.location && (
                      <div className="flex items-center gap-1.5 text-xs mt-3" style={{ color: "var(--muted-ink)" }}>
                        <span>📍</span>
                        <span>{moment.location}</span>
                      </div>
                    )}

                    <div className="flex items-center justify-between mt-4">
                      <span className="text-xs md:block hidden" style={{ color: "var(--muted-ink)" }}>{toDateText(moment.date)}</span>
                      <span className="text-xs" style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>第 {idx + 1} 个瞬间</span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          {moments.length === 0 && (
            <div className="py-20 text-center">
              <p style={{ color: "var(--muted-ink)" }}>{loading ? "正在翻阅生活瞬间..." : "生活瞬间暂时还是空白"}</p>
            </div>
          )}

          <AppPagination page={page} totalPages={totalPages} onPageChange={setPage} />
        </div>
      </div>
    </PageLayout>
  );
}
