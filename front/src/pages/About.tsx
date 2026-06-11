import {
  BookOpenIcon,
  CameraIcon,
  CoffeeIcon,
  HeartIcon,
  MailIcon,
  MapPinIcon,
  MusicIcon,
  PenToolIcon,
  SparklesIcon,
  type LucideIcon,
} from "lucide-react";
import { Link } from "react-router-dom";
import PageLayout from "../components/layout/PageLayout";
import { useSiteProfile } from "../features/site/useSiteProfile";

function splitParagraphs(value: string): string[] {
  return value
    .split(/\n\s*\n|\n/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function resolveStatusIcon(value: string): LucideIcon | null {
  const normalized = value.trim().toLowerCase();
  if (!normalized) return null;
  if (normalized.includes("camera")) return CameraIcon;
  if (normalized.includes("heart")) return HeartIcon;
  if (normalized.includes("coffee") || normalized.includes("cup")) return CoffeeIcon;
  if (normalized.includes("map")) return MapPinIcon;
  if (normalized.includes("music")) return MusicIcon;
  if (normalized.includes("pen") || normalized.includes("quill")) return PenToolIcon;
  if (normalized.includes("spark")) return SparklesIcon;
  return BookOpenIcon;
}

function isImageIcon(value: string): boolean {
  return /^(https?:)?\/\//.test(value) || value.startsWith("/");
}

function renderStatusIcon(value: string) {
  if (isImageIcon(value)) {
    return <img src={value} alt="" className="h-4 w-4 object-contain" />;
  }

  const Icon = resolveStatusIcon(value);
  return Icon ? <Icon size={14} /> : <span className="h-2 w-2 rounded-full bg-[var(--clay)]" />;
}

function initialsFromName(value: string): string {
  const trimmed = value.trim();
  if (!trimmed) return "";
  return trimmed.slice(0, 2).toUpperCase();
}

export default function About() {
  const { profile } = useSiteProfile();
  const displayName = profile.ownerDisplayName || profile.title;
  const introParagraphs = splitParagraphs(profile.aboutIntro);
  const hasAvatar = Boolean(profile.avatarUrl);
  const hasStatusItems = profile.aboutStatusItems.length > 0;
  const hasSkillItems = profile.aboutSkillItems.length > 0;
  const hasTimelineItems = profile.aboutTimelineItems.length > 0;
  const showDetailsSection = hasSkillItems || hasTimelineItems;

  return (
    <PageLayout>
      <div className="mx-auto max-w-7xl px-8 pt-16 pb-24">
        <div className="mb-20 flex flex-wrap items-start gap-12 md:flex-nowrap">
          <div className="min-w-64 flex-1">
            <p
              className="mb-4 text-xs uppercase tracking-widest"
              style={{ color: "var(--muted-ink)" }}
            >
              About
            </p>
            <h1
              className="mb-8 leading-tight"
              style={{
                fontFamily: "var(--fontDisplay)",
                fontSize: "clamp(36px,4vw,58px)",
                fontWeight: 400,
                color: "var(--ink)",
                lineHeight: 1.2,
              }}
            >
              你好，
              <br />
              这里是
              <em style={{ fontStyle: "italic", color: "var(--clay)" }}>
                {displayName}
              </em>
            </h1>

            {introParagraphs.length > 0 && (
              <div
                className="mb-8 max-w-[560px] text-base leading-relaxed"
                style={{
                  fontFamily: "var(--fontBody)",
                  color: "var(--ink)",
                  lineHeight: 2,
                }}
              >
                {introParagraphs.map((paragraph) => (
                  <p key={paragraph} className="mb-4 last:mb-0">
                    {paragraph}
                  </p>
                ))}
              </div>
            )}

            <div className="flex items-center gap-4">
              {profile.email && (
                <a
                  href={`mailto:${profile.email}`}
                  className="inline-flex items-center gap-2 px-5 py-2.5 text-sm transition-opacity hover:opacity-80"
                  style={{
                    background: "var(--ink)",
                    color: "var(--warm-white)",
                    fontFamily: "var(--fontSans)",
                  }}
                >
                  <MailIcon size={14} /> 联系我
                </a>
              )}
              <Link
                to="/guestbook"
                className="inline-flex items-center gap-2 border px-5 py-2.5 text-sm transition-colors hover:border-primary"
                style={{
                  borderColor: "var(--warm-border)",
                  color: "var(--muted-ink)",
                  fontFamily: "var(--fontSans)",
                }}
              >
                留言
              </Link>
            </div>
          </div>

          <div className="shrink-0" style={{ width: "300px" }}>
            <div className="overflow-hidden" style={{ height: "380px" }}>
              {hasAvatar ? (
                <img
                  src={profile.avatarUrl}
                  alt={displayName}
                  className="h-full w-full object-cover"
                />
              ) : (
                <div
                  className="flex h-full w-full items-center justify-center"
                  style={{
                    background:
                      "linear-gradient(135deg, rgba(242,239,234,0.98), rgba(228,224,217,0.82))",
                    color: "var(--muted-ink)",
                  }}
                >
                  <span
                    style={{
                      fontFamily: "var(--fontDisplay)",
                      fontSize: "42px",
                      letterSpacing: "0.08em",
                    }}
                  >
                    {initialsFromName(displayName)}
                  </span>
                </div>
              )}
            </div>

            {hasStatusItems && (
              <div
                className="p-5 text-sm"
                style={{
                  background: "var(--section-bg)",
                  border: "1px solid var(--warm-border)",
                }}
              >
                <div className="flex flex-col gap-2">
                  {profile.aboutStatusItems.map((item) => (
                    <div
                      key={`${item.label}-${item.content}`}
                      className="flex items-center gap-2"
                      style={{ color: "var(--muted-ink)" }}
                    >
                      {renderStatusIcon(item.icon)}
                      <span>
                        {item.label}：{item.content}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>

        {showDetailsSection && (
          <>
            <div className="mb-16 border-t" style={{ borderColor: "var(--warm-border)" }} />

            <div className="flex flex-wrap gap-16 md:flex-nowrap">
              {hasSkillItems && (
                <div className="min-w-64 flex-1">
                  <h2
                    className="mb-8"
                    style={{
                      fontFamily: "var(--fontDisplay)",
                      fontSize: "22px",
                      fontWeight: 400,
                      color: "var(--ink)",
                    }}
                  >
                    特长与爱好
                  </h2>
                  <div className="flex flex-col gap-5">
                    {profile.aboutSkillItems.map((item) => {
                      const numeric = Number(item.value);
                      const percent = Number.isFinite(numeric)
                        ? Math.max(0, Math.min(100, numeric))
                        : null;

                      return (
                        <div key={`${item.name}-${item.value}`}>
                          <div className="mb-2 flex justify-between">
                            <span
                              className="text-sm"
                              style={{
                                color: "var(--ink)",
                                fontFamily: "var(--fontSans)",
                              }}
                            >
                              {item.name}
                            </span>
                            <span className="text-xs" style={{ color: "var(--muted-ink)" }}>
                              {percent === null ? item.value : `${percent}%`}
                            </span>
                          </div>
                          <div
                            className="h-1.5 w-full overflow-hidden"
                            style={{ background: "var(--section-bg)" }}
                          >
                            <div
                              className="h-full"
                              style={{
                                width: `${percent ?? 0}%`,
                                background: "var(--ink)",
                              }}
                            />
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {hasTimelineItems && (
                <div className="min-w-64 flex-1">
                  <h2
                    className="mb-8"
                    style={{
                      fontFamily: "var(--fontDisplay)",
                      fontSize: "22px",
                      fontWeight: 400,
                      color: "var(--ink)",
                    }}
                  >
                    时间轴
                  </h2>
                  <div className="relative">
                    <div
                      className="absolute bottom-0 left-12 top-0 w-px"
                      style={{ background: "var(--warm-border)" }}
                    />
                    <div className="flex flex-col gap-8">
                      {profile.aboutTimelineItems.map((item) => (
                        <div
                          key={`${item.year}-${item.event}`}
                          className="flex items-start gap-6"
                        >
                          <div
                            className="w-24 shrink-0 pt-0.5 text-right text-xs"
                            style={{
                              fontFamily: "var(--fontDisplay)",
                              fontSize: "15px",
                              color: "var(--clay)",
                              fontWeight: 600,
                            }}
                          >
                            {item.year}
                          </div>
                          <div
                            className="mt-0.5 h-2.5 w-2.5 shrink-0 rounded-full"
                            style={{
                              background: "var(--clay)",
                              outline: "3px solid var(--ivory)",
                              outlineOffset: "1px",
                            }}
                          />
                          <p
                            className="text-sm leading-relaxed"
                            style={{
                              color: "var(--ink)",
                              fontFamily: "var(--fontSans)",
                            }}
                          >
                            {item.event}
                          </p>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </div>
          </>
        )}

        {profile.aboutBottomQuote && (
          <div
            className="mt-16 border-t py-8 text-center"
            style={{ borderColor: "var(--warm-border)" }}
          >
            <p
              className="text-sm"
              style={{
                fontFamily: "var(--fontDisplay)",
                fontStyle: "italic",
                color: "var(--muted-ink)",
                fontSize: "16px",
              }}
            >
              "{profile.aboutBottomQuote}"
            </p>
          </div>
        )}
      </div>
    </PageLayout>
  );
}
