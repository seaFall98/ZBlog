import {
  ActivityIcon,
  ArchiveIcon,
  BookOpenTextIcon,
  CameraIcon,
  FolderIcon,
  LinkIcon,
  MessageSquareIcon,
  MousePointerClickIcon,
  RadioIcon,
  TagIcon,
} from "lucide-react";
import type { ReactNode } from "react";
import PageLayout from "../components/layout/PageLayout";
import { useSiteStats } from "../features/stats/useSiteStats";

function formatNumber(value: number) {
  return value.toLocaleString("zh-CN");
}

function formatWords(value: number) {
  if (value >= 10000) {
    return `${(value / 10000).toFixed(value >= 100000 ? 0 : 1)} 万`;
  }
  return formatNumber(value);
}

type MetricCardProps = {
  label: string;
  value: string | number;
  icon: ReactNode;
  hint?: string;
};

function MetricCard({ label, value, icon, hint }: MetricCardProps) {
  return (
    <div
      className="min-h-36 p-6 transition-transform duration-300 hover:-translate-y-0.5"
      style={{ background: "var(--warm-white)", border: "1px solid var(--warm-border)" }}
    >
      <div className="mb-8 flex items-center justify-between gap-4">
        <span className="text-sm" style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>
          {label}
        </span>
        <span
          className="flex h-9 w-9 items-center justify-center rounded-sm"
          style={{ background: "var(--section-bg)", color: "var(--olive)" }}
        >
          {icon}
        </span>
      </div>
      <div
        style={{
          color: "var(--ink)",
          fontFamily: "var(--fontDisplay)",
          fontSize: "clamp(30px,4vw,46px)",
          fontWeight: 700,
          lineHeight: 1,
        }}
      >
        {typeof value === "number" ? formatNumber(value) : value}
      </div>
      {hint && (
        <div className="mt-3 text-xs" style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>
          {hint}
        </div>
      )}
    </div>
  );
}

type RowMetricProps = {
  label: string;
  value: number;
};

function RowMetric({ label, value }: RowMetricProps) {
  return (
    <div className="flex items-baseline justify-between gap-4 py-4" style={{ borderBottom: "1px solid var(--warm-border)" }}>
      <span className="text-sm" style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>
        {label}
      </span>
      <span
        className="text-lg"
        style={{ color: "var(--ink)", fontFamily: "var(--fontDisplay)", fontWeight: 600 }}
      >
        {formatNumber(value)}
      </span>
    </div>
  );
}

export default function Stats() {
  const stats = useSiteStats();

  const headlineMetrics = [
    {
      label: "文章",
      value: stats.totalArticles,
      icon: <BookOpenTextIcon size={18} />,
      hint: `${formatWords(stats.totalWords)} 字已发布`,
    },
    {
      label: "访问",
      value: stats.totalVisits,
      icon: <MousePointerClickIcon size={18} />,
      hint: `${formatNumber(stats.totalVisitors)} 位访客`,
    },
    {
      label: "今日",
      value: stats.todayPageviews,
      icon: <ActivityIcon size={18} />,
      hint: `${formatNumber(stats.todayVisitors)} 位访客`,
    },
    {
      label: "本月",
      value: stats.monthPageviews,
      icon: <ArchiveIcon size={18} />,
      hint: "本月浏览量",
    },
  ];

  const contentMetrics = [
    { label: "留言", value: stats.totalMessages, icon: <MessageSquareIcon size={16} /> },
    { label: "动态", value: stats.totalMoments, icon: <RadioIcon size={16} /> },
    { label: "照片", value: stats.totalPhotos, icon: <CameraIcon size={16} /> },
    { label: "分类", value: stats.totalCategories, icon: <FolderIcon size={16} /> },
    { label: "标签", value: stats.totalTags, icon: <TagIcon size={16} /> },
    { label: "友链", value: stats.totalFriends, icon: <LinkIcon size={16} /> },
  ];

  return (
    <PageLayout>
      <div className="mx-auto max-w-7xl px-8 pt-16 pb-24">
        <section className="mb-12">
          <div>
            <p className="mb-4 text-xs uppercase tracking-widest" style={{ color: "var(--muted-ink)" }}>
              Statistics
            </p>
            <h1
              style={{
                color: "var(--ink)",
                fontFamily: "var(--fontDisplay)",
                fontSize: "clamp(38px,5vw,68px)",
                fontWeight: 400,
                lineHeight: 1.08,
              }}
            >
              站点统计
            </h1>
          </div>
        </section>

        {stats.error && (
          <div className="mb-8 p-5 text-sm" style={{ background: "var(--warm-white)", border: "1px solid var(--warm-border)", color: "var(--muted-ink)" }}>
            统计数据暂时读取失败，请稍后刷新。
          </div>
        )}

        <section className="mb-12 grid gap-5 md:grid-cols-2 xl:grid-cols-4">
          {headlineMetrics.map((metric) => (
            <MetricCard key={metric.label} {...metric} />
          ))}
        </section>

        <section className="grid gap-8 lg:grid-cols-[minmax(0,1.1fr)_minmax(300px,0.9fr)]">
          <div
            className="p-7"
            style={{ background: "var(--warm-white)", border: "1px solid var(--warm-border)" }}
          >
            <h2
              className="mb-2"
              style={{ color: "var(--ink)", fontFamily: "var(--fontDisplay)", fontSize: "26px", fontWeight: 400 }}
            >
              访问概览
            </h2>
            <div className="mt-4">
              <RowMetric label="今日浏览" value={stats.todayPageviews} />
              <RowMetric label="昨日浏览" value={stats.yesterdayPageviews} />
              <RowMetric label="今日访客" value={stats.todayVisitors} />
              <RowMetric label="昨日访客" value={stats.yesterdayVisitors} />
              <RowMetric label="在线访客" value={stats.onlineUsers} />
              <RowMetric label="本月浏览" value={stats.monthPageviews} />
              <RowMetric label="累计浏览" value={stats.totalVisits} />
            </div>
          </div>

          <div
            className="p-7"
            style={{ background: "var(--warm-white)", border: "1px solid var(--warm-border)" }}
          >
            <h2
              className="mb-6"
              style={{ color: "var(--ink)", fontFamily: "var(--fontDisplay)", fontSize: "26px", fontWeight: 400 }}
            >
              内容资产
            </h2>
            <div className="grid grid-cols-2 gap-3">
              {contentMetrics.map((metric) => (
                <div
                  key={metric.label}
                  className="p-4"
                  style={{ background: "var(--section-bg)", border: "1px solid var(--warm-border)" }}
                >
                  <div className="mb-5 flex items-center justify-between gap-3" style={{ color: "var(--muted-ink)" }}>
                    <span className="text-sm">{metric.label}</span>
                    {metric.icon}
                  </div>
                  <div
                    style={{ color: "var(--ink)", fontFamily: "var(--fontDisplay)", fontSize: "30px", fontWeight: 700 }}
                  >
                    {formatNumber(metric.value)}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>

        {stats.loading && (
          <div className="mt-8 text-xs" style={{ color: "var(--muted-ink)" }}>
            正在读取最新统计。
          </div>
        )}
      </div>
    </PageLayout>
  );
}
