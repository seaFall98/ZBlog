import {
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import { Link } from "react-router-dom";
import { TrendingUpIcon, FileTextIcon, ImageIcon, MessageSquareIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";
import { siteStats } from "../data/mockData";

export default function Stats() {
  const statCards = [
    {
      label: "文章总数",
      value: siteStats.totalPosts,
      icon: <FileTextIcon size={18} />,
      color: "var(--olive)",
      link: "/blog",
    },
    {
      label: "累计访问",
      value: siteStats.totalVisits.toLocaleString(),
      icon: <TrendingUpIcon size={18} />,
      color: "var(--clay)",
      link: "/",
    },
    {
      label: "照片数量",
      value: siteStats.totalPhotos,
      icon: <ImageIcon size={18} />,
      color: "var(--slate-blue)",
      link: "/gallery",
    },
    {
      label: "留言数量",
      value: siteStats.totalMessages,
      icon: <MessageSquareIcon size={18} />,
      color: "var(--ink)",
      link: "/guestbook",
    },
  ];

  return (
    <PageLayout>
      <div className="max-w-7xl mx-auto px-8 pt-16 pb-24">
        {/* Header */}
        <div className="mb-14">
          <p className="text-xs tracking-widest uppercase mb-3" style={{ color: "var(--muted-ink)" }}>Dashboard</p>
          <h1 style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(36px,4vw,56px)", fontWeight: 400, color: "var(--ink)" }}>
            站点统计
          </h1>
          <p className="mt-4 text-sm" style={{ color: "var(--muted-ink)" }}>数据记录博客成长的足迹</p>
        </div>

        {/* Stat cards */}
        <div className="flex gap-5 flex-wrap mb-14">
          {statCards.map((card) => (
            <Link
              key={card.label}
              to={card.link}
              className="group flex-1 min-w-44 block p-7 hover:-translate-y-1 transition-transform duration-300"
              style={{ background: "var(--warm-white)", border: "1px solid var(--warm-border)" }}
            >
              <div
                className="w-10 h-10 flex items-center justify-center rounded-sm mb-5"
                style={{ background: "var(--section-bg)", color: card.color }}
              >
                {card.icon}
              </div>
              <div
                style={{ fontFamily: "var(--fontDisplay)", fontSize: "36px", fontWeight: 700, color: "var(--ink)", lineHeight: 1 }}
              >
                {card.value}
              </div>
              <div className="mt-2 text-xs" style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>
                {card.label}
              </div>
            </Link>
          ))}
        </div>

        {/* Charts row */}
        <div className="flex gap-6 flex-wrap md:flex-nowrap mb-12">
          {/* Line chart: monthly visits */}
          <div
            className="flex-1 min-w-64 p-7"
            style={{ background: "var(--warm-white)", border: "1px solid var(--warm-border)" }}
          >
            <h3
              className="mb-6 text-sm font-medium"
              style={{ color: "var(--ink)", fontFamily: "var(--fontSans)" }}
            >
              月度访问趋势
            </h3>
            <ResponsiveContainer width="100%" height={220}>
              <LineChart data={siteStats.monthlyVisits} margin={{ top: 5, right: 5, left: -30, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--warm-border)" />
                <XAxis
                  dataKey="month"
                  tick={{ fontSize: 11, fill: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}
                />
                <YAxis tick={{ fontSize: 11, fill: "var(--muted-ink)" }} />
                <Tooltip
                  contentStyle={{
                    background: "var(--warm-white)",
                    border: "1px solid var(--warm-border)",
                    fontFamily: "var(--fontSans)",
                    fontSize: "12px",
                    color: "var(--ink)",
                  }}
                />
                <Line
                  type="monotone"
                  dataKey="visits"
                  stroke="#7A8C6E"
                  strokeWidth={2}
                  dot={{ r: 3, fill: "#7A8C6E" }}
                  name="访问量"
                />
              </LineChart>
            </ResponsiveContainer>
          </div>

          {/* Bar chart: monthly posts */}
          <div
            className="flex-1 min-w-64 p-7"
            style={{ background: "var(--warm-white)", border: "1px solid var(--warm-border)" }}
          >
            <h3
              className="mb-6 text-sm font-medium"
              style={{ color: "var(--ink)", fontFamily: "var(--fontSans)" }}
            >
              月度发文数量
            </h3>
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={siteStats.monthlyVisits} margin={{ top: 5, right: 5, left: -30, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--warm-border)" />
                <XAxis
                  dataKey="month"
                  tick={{ fontSize: 11, fill: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}
                />
                <YAxis tick={{ fontSize: 11, fill: "var(--muted-ink)" }} />
                <Tooltip
                  contentStyle={{
                    background: "var(--warm-white)",
                    border: "1px solid var(--warm-border)",
                    fontFamily: "var(--fontSans)",
                    fontSize: "12px",
                    color: "var(--ink)",
                  }}
                />
                <Bar dataKey="posts" fill="#B5956A" name="文章数" radius={[2, 2, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Top posts */}
        <div
          className="p-7"
          style={{ background: "var(--warm-white)", border: "1px solid var(--warm-border)" }}
        >
          <h3
            className="mb-6 text-sm font-medium"
            style={{ color: "var(--ink)", fontFamily: "var(--fontSans)" }}
          >
            热门文章 Top 5
          </h3>
          <div className="flex flex-col gap-0">
            {siteStats.topPosts.map((post, idx) => {
              const maxViews = Math.max(...siteStats.topPosts.map((p) => p.views));
              const pct = (post.views / maxViews) * 100;
              return (
                <Link
                  key={post.id}
                  to={`/blog/${post.id}`}
                  className="flex items-center gap-4 py-4 border-b group hover:opacity-70 transition-opacity"
                  style={{ borderColor: "var(--warm-border)" }}
                >
                  <span
                    className="shrink-0 text-xs w-6"
                    style={{
                      fontFamily: "var(--fontDisplay)",
                      color: idx < 3 ? "var(--clay)" : "var(--muted-ink)",
                      fontWeight: idx < 3 ? 700 : 400,
                    }}
                  >
                    {idx + 1}
                  </span>
                  <span
                    className="flex-1 min-w-0 text-sm truncate"
                    style={{ color: "var(--ink)", fontFamily: "var(--fontSans)" }}
                  >
                    {post.title}
                  </span>
                  {/* Bar */}
                  <div
                    className="hidden md:block w-32 h-1.5 overflow-hidden shrink-0"
                    style={{ background: "var(--section-bg)" }}
                  >
                    <div
                      className="h-full"
                      style={{ width: `${pct}%`, background: "var(--olive)" }}
                    />
                  </div>
                  <span className="shrink-0 text-xs" style={{ color: "var(--muted-ink)" }}>
                    {post.views.toLocaleString()} 次
                  </span>
                </Link>
              );
            })}
          </div>
        </div>
      </div>
    </PageLayout>
  );
}
