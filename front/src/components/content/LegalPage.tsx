import { Link } from "react-router-dom";
import PageLayout from "../layout/PageLayout";

export type LegalSection = {
  title: string;
  paragraphs?: string[];
  bullets?: string[];
};

type LegalPageProps = {
  eyebrow: string;
  title: string;
  lead: string;
  updatedAt: string;
  sections: LegalSection[];
  contactEmail?: string;
};

function withContact(value: string, contactEmail?: string): string {
  if (!contactEmail) return value.replaceAll("{contactEmail}", "站点管理员");
  return value.replaceAll("{contactEmail}", contactEmail);
}

export default function LegalPage({
  eyebrow,
  title,
  lead,
  updatedAt,
  sections,
  contactEmail,
}: LegalPageProps) {
  return (
    <PageLayout>
      <div className="mx-auto max-w-5xl px-8 pt-16 pb-24">
        <header className="mb-14 max-w-3xl">
          <p
            className="mb-4 text-xs uppercase tracking-[0.22em]"
            style={{ color: "var(--muted-ink)" }}
          >
            {eyebrow}
          </p>
          <h1
            style={{
              fontFamily: "var(--fontDisplay)",
              fontSize: "clamp(38px,4.6vw,64px)",
              fontWeight: 400,
              color: "var(--ink)",
              lineHeight: 1.08,
            }}
          >
            {title}
          </h1>
          <p
            className="mt-6 text-base leading-8"
            style={{ color: "var(--muted-ink)", fontFamily: "var(--fontBody)" }}
          >
            {withContact(lead, contactEmail)}
          </p>
        </header>

        <div
          className="grid gap-12 border-t pt-12 md:grid-cols-[180px_minmax(0,1fr)]"
          style={{ borderColor: "var(--warm-border)" }}
        >
          <aside className="text-sm" style={{ color: "var(--muted-ink)" }}>
            <div className="sticky top-28 space-y-3">
              <p className="uppercase tracking-[0.18em]">Legal</p>
              <p>最后更新：{updatedAt}</p>
              <div className="space-y-2">
                <Link to="/privacy" className="block transition-colors hover:text-primary">
                  隐私政策
                </Link>
                <Link to="/cookies" className="block transition-colors hover:text-primary">
                  Cookies
                </Link>
                <Link to="/copyright" className="block transition-colors hover:text-primary">
                  版权声明
                </Link>
              </div>
            </div>
          </aside>

          <div className="space-y-10">
            {sections.map((section) => (
              <section key={section.title} className="space-y-4">
                <h2
                  style={{
                    fontFamily: "var(--fontDisplay)",
                    fontSize: "30px",
                    fontWeight: 400,
                    color: "var(--ink)",
                  }}
                >
                  {section.title}
                </h2>
                {section.paragraphs?.map((paragraph) => (
                  <p
                    key={paragraph}
                    className="text-[15px] leading-8"
                    style={{ color: "var(--ink)", fontFamily: "var(--fontBody)" }}
                  >
                    {withContact(paragraph, contactEmail)}
                  </p>
                ))}
                {section.bullets && section.bullets.length > 0 && (
                  <ul
                    className="space-y-3 pl-5 text-[15px]"
                    style={{ color: "var(--ink)", fontFamily: "var(--fontBody)" }}
                  >
                    {section.bullets.map((item) => (
                      <li key={item} className="list-disc leading-8">
                        {withContact(item, contactEmail)}
                      </li>
                    ))}
                  </ul>
                )}
              </section>
            ))}
          </div>
        </div>
      </div>
    </PageLayout>
  );
}
