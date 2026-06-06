import PageLayout from "../components/layout/PageLayout";
import { moments } from "../data/mockData";

const moodColors: Record<string, string> = {
  慵懒: "#B5956A",
  满足: "#7A8C6E",
  平静: "#7D8FA3",
  好奇: "#9B7FB6",
  感动: "#C0786A",
};

export default function Moments() {
  return (
    <PageLayout>
      <div className="max-w-5xl mx-auto px-8 pt-16 pb-24">
        {/* Header */}
        <div className="mb-14">
          <p className="text-xs tracking-widest uppercase mb-3" style={{ color: "var(--muted-ink)" }}>Moments</p>
          <h1 style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(36px,4vw,56px)", fontWeight: 400, color: "var(--ink)" }}>
            生活瞬间
          </h1>
          <p className="mt-4 text-sm" style={{ color: "var(--muted-ink)" }}>记录生命中不想遗忘的碎片</p>
        </div>

        {/* Feed */}
        <div className="relative">
          {/* Vertical line */}
          <div
            className="absolute left-16 top-0 bottom-0 w-px hidden md:block"
            style={{ background: "var(--warm-border)" }}
          />

          <div className="flex flex-col gap-0">
            {moments.map((moment, idx) => {
              const moodColor = moodColors[moment.mood] ?? "var(--muted-ink)";
              return (
                <div key={moment.id} className="relative flex gap-8">
                  {/* Date column */}
                  <div className="hidden md:flex flex-col items-center pt-6 shrink-0 w-32">
                    <div
                      className="w-2.5 h-2.5 rounded-full shrink-0 ring-2"
                      style={{
                        background: moodColor,
                        outline: `3px solid var(--ivory)`,
                        outlineOffset: "1px",
                        zIndex: 1,
                      }}
                    />
                    <div className="text-xs mt-2 text-center" style={{ color: "var(--muted-ink)" }}>
                      {moment.date.slice(5)}
                    </div>
                  </div>

                  {/* Card */}
                  <div
                    className="flex-1 mb-8 p-6"
                    style={{
                      background: "var(--warm-white)",
                      border: "1px solid var(--warm-border)",
                    }}
                  >
                    {/* Mobile date */}
                    <div className="flex items-center gap-3 mb-3 md:hidden">
                      <span className="text-xs" style={{ color: "var(--muted-ink)" }}>{moment.date}</span>
                    </div>

                    {/* Mood badge */}
                    <div className="flex items-center gap-2 mb-4">
                      <span
                        className="w-2 h-2 rounded-full inline-block"
                        style={{ background: moodColor }}
                      />
                      <span
                        className="text-xs"
                        style={{ color: moodColor, fontFamily: "var(--fontSans)" }}
                      >
                        {moment.mood}
                      </span>
                    </div>

                    {/* Text */}
                    <p
                      className="leading-relaxed"
                      style={{
                        fontFamily: "var(--fontBody)",
                        fontSize: "15px",
                        color: "var(--ink)",
                        lineHeight: 1.95,
                      }}
                    >
                      {moment.text}
                    </p>

                    {/* Images */}
                    {(moment.images ?? []).length > 0 && (
                      <div className="flex gap-2 mt-5 flex-wrap">
                        {(moment.images ?? []).map((src, imgIdx) => (
                          <div
                            key={imgIdx}
                            className="overflow-hidden"
                            style={{
                              width: moment.images.length === 1 ? "100%" : "calc(50% - 4px)",
                              height: "180px",
                            }}
                          >
                            <img
                              src={src}
                              alt=""
                              loading="lazy"
                              className="w-full h-full object-cover"
                            />
                          </div>
                        ))}
                      </div>
                    )}

                    {/* Footer */}
                    <div className="flex items-center justify-between mt-4">
                      <span className="text-xs md:block hidden" style={{ color: "var(--muted-ink)" }}>{moment.date}</span>
                      <span className="text-xs" style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>
                        第 {idx + 1} 个瞬间
                      </span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </PageLayout>
  );
}
