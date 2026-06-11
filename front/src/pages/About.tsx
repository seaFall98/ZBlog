import { Link } from "react-router-dom";
import { BookOpenIcon, CameraIcon, HeartIcon, MailIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";
import { useSiteProfile } from "../features/site/useSiteProfile";

type ProfileItem = {
  label: string;
  value: string;
  color?: string;
};

type TimelineItem = {
  year: string;
  event: string;
};

const defaultProfileItems: ProfileItem[] = [
  { label: "正在读", value: "《百年孤独》" },
  { label: "最近在拍", value: "秋日街景" },
  { label: "最近喜欢", value: "煮咖啡" },
  { label: "写作", value: "90" },
  { label: "摄影", value: "75" },
  { label: "阅读", value: "95" },
  { label: "旅行", value: "80" },
];

const defaultTimeline: TimelineItem[] = [
  { year: "2024", event: "开始认真记录，坚持每周更新" },
  { year: "2023", event: "第一次独自旅行，去了日本京都" },
  { year: "2022", event: "买了第一台相机，开始认真学摄影" },
  { year: "2021", event: "建立这个博客，写下第一篇文章" },
];

function splitParagraphs(value: string): string[] {
  return value
    .split(/\n\s*\n|\n/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function parseJsonArray<T>(value: string): T[] {
  if (!value.trim()) return [];
  try {
    const parsed = JSON.parse(value) as unknown;
    return Array.isArray(parsed) ? (parsed as T[]) : [];
  } catch {
    return [];
  }
}

function normalizeProfileItems(value: string): ProfileItem[] {
  return parseJsonArray<Partial<ProfileItem>>(value)
    .map((item) => ({
      label: String(item.label ?? "").trim(),
      value: String(item.value ?? "").trim(),
      color: String(item.color ?? "").trim(),
    }))
    .filter((item) => item.label && item.value);
}

function normalizeTimeline(value: string): TimelineItem[] {
  const parsed = parseJsonArray<Partial<TimelineItem> & { label?: unknown; value?: unknown }>(value)
    .map((item) => ({ year: String(item.year ?? item.label ?? "").trim(), event: String(item.event ?? item.value ?? "").trim() }))
    .filter((item) => item.year && item.event);
  if (parsed.length > 0) return parsed;

  return value
    .split("\n")
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [year, ...eventParts] = line.split(/[:：]/);
      return { year: year?.trim() ?? "", event: eventParts.join("：").trim() };
    })
    .filter((item) => item.year && item.event);
}

function statusIcon(label: string) {
  if (label.includes("拍")) return CameraIcon;
  if (label.includes("喜欢")) return HeartIcon;
  return BookOpenIcon;
}

export default function About() {
  const { profile } = useSiteProfile();
  const displayName = profile.title || "寂静之书";
  const introSource = profile.aboutDescribe || profile.aboutIntro || "一个喜欢在平凡生活里寻找微小美好的人。";
  const introParagraphs = splitParagraphs(introSource);
  const configuredProfileItems = normalizeProfileItems(profile.aboutProfile);
  const profileItems = configuredProfileItems.length > 0 ? configuredProfileItems : defaultProfileItems;
  const statusItems = profileItems.slice(0, 3);
  const skillItems = profileItems.slice(3).length > 0 ? profileItems.slice(3) : profileItems;
  const timeline = normalizeTimeline(profile.aboutStory);
  const timelineItems = timeline.length > 0 ? timeline : defaultTimeline;
  const mottoLines = parseJsonArray<string>(profile.aboutMottoMain).filter(Boolean);
  const quoteText = mottoLines.length > 0 ? mottoLines.join(" ") : (profile.aboutMottoSub || "生活就是很多很多个平凡的日子，偶尔有一些光。");

  return (
    <PageLayout>
      <div className="max-w-7xl mx-auto px-8 pt-16 pb-24">
        <div className="flex gap-12 flex-wrap md:flex-nowrap items-start mb-20">
          <div className="flex-1 min-w-64">
            <p className="text-xs tracking-widest uppercase mb-4" style={{ color: "var(--muted-ink)" }}>About</p>
            <h1 className="mb-8 leading-tight" style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(36px,4vw,58px)", fontWeight: 400, color: "var(--ink)", lineHeight: 1.2 }}>
              你好，<br />
              这里是<em style={{ fontStyle: "italic", color: "var(--clay)" }}>{displayName}</em>
            </h1>
            <div className="leading-relaxed mb-8 text-base" style={{ fontFamily: "var(--fontBody)", color: "var(--ink)", lineHeight: 2, maxWidth: "560px" }}>
              {introParagraphs.map((paragraph) => (
                <p key={paragraph} className="mb-4 last:mb-0">{paragraph}</p>
              ))}
            </div>
            <div className="flex items-center gap-4">
              <a href={`mailto:${profile.email || "hello@quietbook.me"}`} className="inline-flex items-center gap-2 text-sm px-5 py-2.5 transition-opacity hover:opacity-80" style={{ background: "var(--ink)", color: "var(--warm-white)", fontFamily: "var(--fontSans)" }}>
                <MailIcon size={14} /> 联系我
              </a>
              <Link to="/guestbook" className="inline-flex items-center gap-2 text-sm px-5 py-2.5 border transition-colors hover:border-primary" style={{ borderColor: "var(--warm-border)", color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>
                留言
              </Link>
            </div>
          </div>

          <div className="shrink-0" style={{ width: "300px" }}>
            <div className="overflow-hidden" style={{ height: "380px" }}>
              <img src={profile.avatarUrl || "https://images.unsplash.com/photo-1529665253569-6d01c0eaf7b6?w=600&q=80"} alt="头像" className="w-full h-full object-cover" />
            </div>
            <div className="p-5 text-sm" style={{ background: "var(--section-bg)", border: "1px solid var(--warm-border)" }}>
              <div className="flex flex-col gap-2">
                {statusItems.map((item) => {
                  const Icon = statusIcon(item.label);
                  return (
                    <div key={`${item.label}-${item.value}`} className="flex items-center gap-2" style={{ color: "var(--muted-ink)" }}>
                      <Icon size={13} /> {item.label}：{item.value}
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </div>

        <div className="border-t mb-16" style={{ borderColor: "var(--warm-border)" }} />

        <div className="flex gap-16 flex-wrap md:flex-nowrap">
          <div className="flex-1 min-w-64">
            <h2 className="mb-8" style={{ fontFamily: "var(--fontDisplay)", fontSize: "22px", fontWeight: 400, color: "var(--ink)" }}>特长 & 爱好</h2>
            <div className="flex flex-col gap-5">
              {skillItems.map((item) => {
                const numeric = Number(item.value);
                const percent = Number.isFinite(numeric) ? Math.max(0, Math.min(100, numeric)) : 72;
                return (
                  <div key={`${item.label}-${item.value}`}>
                    <div className="flex justify-between mb-2"><span className="text-sm" style={{ color: "var(--ink)", fontFamily: "var(--fontSans)" }}>{item.label}</span><span className="text-xs" style={{ color: "var(--muted-ink)" }}>{Number.isFinite(numeric) ? `${percent}%` : item.value}</span></div>
                    <div className="w-full h-1.5 overflow-hidden" style={{ background: "var(--section-bg)", borderRadius: 0 }}><div className="h-full" style={{ width: `${percent}%`, background: item.color || "var(--ink)", borderRadius: 0 }} /></div>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="flex-1 min-w-64">
            <h2 className="mb-8" style={{ fontFamily: "var(--fontDisplay)", fontSize: "22px", fontWeight: 400, color: "var(--ink)" }}>时间轴</h2>
            <div className="relative">
              <div className="absolute left-12 top-0 bottom-0 w-px" style={{ background: "var(--warm-border)" }} />
              <div className="flex flex-col gap-8">
                {timelineItems.map((item) => (
                  <div key={`${item.year}-${item.event}`} className="flex gap-6 items-start">
                    <div className="shrink-0 w-24 text-right text-xs pt-0.5" style={{ fontFamily: "var(--fontDisplay)", fontSize: "15px", color: "var(--clay)", fontWeight: 600 }}>{item.year}</div>
                    <div className="shrink-0 w-2.5 h-2.5 rounded-full mt-0.5" style={{ background: "var(--clay)", outline: "3px solid var(--ivory)", outlineOffset: "1px" }} />
                    <p className="text-sm leading-relaxed" style={{ color: "var(--ink)", fontFamily: "var(--fontSans)" }}>{item.event}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        <div className="mt-16 py-8 text-center border-t" style={{ borderColor: "var(--warm-border)" }}>
          <p className="text-sm" style={{ fontFamily: "var(--fontDisplay)", fontStyle: "italic", color: "var(--muted-ink)", fontSize: "16px" }}>"{quoteText}"</p>
          {profile.aboutMottoSub && mottoLines.length > 0 && (
            <p className="mt-3 text-xs" style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>{profile.aboutMottoSub}</p>
          )}
        </div>
      </div>
    </PageLayout>
  );
}
