import { Link } from "react-router-dom";
import { MailIcon, BookOpenIcon, CameraIcon, HeartIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";

const skills = [
  { name: "写作", level: 90 },
  { name: "摄影", level: 75 },
  { name: "阅读", level: 95 },
  { name: "旅行", level: 80 },
  { name: "设计", level: 65 },
];

const timeline = [
  { year: "2024", event: "开始认真记录，坚持每周更新" },
  { year: "2023", event: "第一次独自旅行，去了日本京都" },
  { year: "2022", event: "买了第一台相机，开始认真学摄影" },
  { year: "2021", event: "建立这个博客，写下第一篇文章" },
];

export default function About() {
  return (
    <PageLayout>
      <div className="max-w-7xl mx-auto px-8 pt-16 pb-24">
        {/* Hero */}
        <div className="flex gap-12 flex-wrap md:flex-nowrap items-start mb-20">
          {/* Left text */}
          <div className="flex-1 min-w-64">
            <p className="text-xs tracking-widest uppercase mb-4" style={{ color: "var(--muted-ink)" }}>About</p>
            <h1
              className="mb-8 leading-tight"
              style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(36px,4vw,58px)", fontWeight: 400, color: "var(--ink)", lineHeight: 1.2 }}
            >
              你好，<br />
              我是<em style={{ fontStyle: "italic", color: "var(--clay)" }}>寂静</em>
            </h1>
            <div
              className="leading-relaxed mb-6 text-base"
              style={{ fontFamily: "var(--fontBody)", color: "var(--ink)", lineHeight: 2, maxWidth: "480px" }}
            >
              <p className="mb-4">
                一个喜欢在平凡生活里寻找微小美好的人。白天是个普通的上班族，晚上是个喜欢写字的人。
              </p>
              <p className="mb-4">
                这个博客是我的私人空间，记录读书的感悟、旅途的光景、日常的碎碎念，以及那些一闪而过、如果不写下来就会忘记的瞬间。
              </p>
              <p>
                相信文字有重量，相信好照片能留住时间，相信生活值得被认真对待。
              </p>
            </div>
            <div className="flex items-center gap-4">
              <a
                href="mailto:hello@quietbook.me"
                className="inline-flex items-center gap-2 text-sm px-5 py-2.5 transition-opacity hover:opacity-80"
                style={{ background: "var(--ink)", color: "var(--warm-white)", fontFamily: "var(--fontSans)" }}
              >
                <MailIcon size={14} /> 联系我
              </a>
              <Link
                to="/guestbook"
                className="inline-flex items-center gap-2 text-sm px-5 py-2.5 border transition-colors hover:border-primary"
                style={{ borderColor: "var(--warm-border)", color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}
              >
                留言
              </Link>
            </div>
          </div>

          {/* Right: portrait */}
          <div className="shrink-0" style={{ width: "300px" }}>
            <div className="overflow-hidden" style={{ height: "380px" }}>
              <img
                src="https://images.unsplash.com/photo-1529665253569-6d01c0eaf7b6?w=600&q=80"
                alt="头像"
                className="w-full h-full object-cover"
              />
            </div>
            <div
              className="p-5 text-sm"
              style={{ background: "var(--section-bg)", border: "1px solid var(--warm-border)" }}
            >
              <div className="flex flex-col gap-2">
                <div className="flex items-center gap-2" style={{ color: "var(--muted-ink)" }}>
                  <BookOpenIcon size={13} /> 正在读：《百年孤独》
                </div>
                <div className="flex items-center gap-2" style={{ color: "var(--muted-ink)" }}>
                  <CameraIcon size={13} /> 最近在拍：秋日街景
                </div>
                <div className="flex items-center gap-2" style={{ color: "var(--muted-ink)" }}>
                  <HeartIcon size={13} /> 最近喜欢：煮咖啡
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Divider */}
        <div className="border-t mb-16" style={{ borderColor: "var(--warm-border)" }} />

        {/* Skills + Timeline in two columns */}
        <div className="flex gap-16 flex-wrap md:flex-nowrap">
          {/* Skills */}
          <div className="flex-1 min-w-64">
            <h2
              className="mb-8"
              style={{ fontFamily: "var(--fontDisplay)", fontSize: "22px", fontWeight: 400, color: "var(--ink)" }}
            >
              特长 & 爱好
            </h2>
            <div className="flex flex-col gap-5">
              {skills.map((s) => (
                <div key={s.name}>
                  <div className="flex justify-between mb-2">
                    <span className="text-sm" style={{ color: "var(--ink)", fontFamily: "var(--fontSans)" }}>{s.name}</span>
                    <span className="text-xs" style={{ color: "var(--muted-ink)" }}>{s.level}%</span>
                  </div>
                  <div
                    className="w-full h-1.5 overflow-hidden"
                    style={{ background: "var(--section-bg)", borderRadius: 0 }}
                  >
                    <div
                      className="h-full"
                      style={{ width: `${s.level}%`, background: "var(--ink)", borderRadius: 0 }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Timeline */}
          <div className="flex-1 min-w-64">
            <h2
              className="mb-8"
              style={{ fontFamily: "var(--fontDisplay)", fontSize: "22px", fontWeight: 400, color: "var(--ink)" }}
            >
              时间轴
            </h2>
            <div className="relative">
              <div
                className="absolute left-12 top-0 bottom-0 w-px"
                style={{ background: "var(--warm-border)" }}
              />
              <div className="flex flex-col gap-8">
                {timeline.map((t) => (
                  <div key={t.year} className="flex gap-6 items-start">
                    <div
                      className="shrink-0 w-24 text-right text-xs pt-0.5"
                      style={{ fontFamily: "var(--fontDisplay)", fontSize: "15px", color: "var(--clay)", fontWeight: 600 }}
                    >
                      {t.year}
                    </div>
                    <div
                      className="shrink-0 w-2.5 h-2.5 rounded-full mt-0.5"
                      style={{ background: "var(--clay)", outline: "3px solid var(--ivory)", outlineOffset: "1px" }}
                    />
                    <p className="text-sm leading-relaxed" style={{ color: "var(--ink)", fontFamily: "var(--fontSans)" }}>
                      {t.event}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Footer note */}
        <div
          className="mt-16 py-8 text-center border-t"
          style={{ borderColor: "var(--warm-border)" }}
        >
          <p
            className="text-sm"
            style={{ fontFamily: "var(--fontDisplay)", fontStyle: "italic", color: "var(--muted-ink)", fontSize: "16px" }}
          >
            "生活就是很多很多个平凡的日子，偶尔有一些光。"
          </p>
        </div>
      </div>
    </PageLayout>
  );
}
