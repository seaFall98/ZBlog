import { Link } from "react-router-dom";
import PageLayout from "../components/layout/PageLayout";

export default function NotFound() {
  return (
    <PageLayout>
      <div className="max-w-3xl mx-auto px-8 pt-24 pb-28 text-center">
        <p className="text-xs tracking-widest uppercase mb-4" style={{ color: "var(--muted-ink)" }}>404</p>
        <h1 style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(36px,4vw,56px)", fontWeight: 400, color: "var(--ink)" }}>
          页面不存在
        </h1>
        <p className="mt-5 text-sm leading-relaxed" style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>
          这个地址没有可展示的内容。
        </p>
        <Link
          to="/"
          className="inline-flex mt-8 px-5 py-2 text-xs border transition-opacity hover:opacity-70"
          style={{ borderColor: "var(--warm-border)", color: "var(--ink)", background: "var(--warm-white)" }}
        >
          返回首页
        </Link>
      </div>
    </PageLayout>
  );
}
