import { useState } from "react";
import { Link } from "react-router-dom";
import { MailIcon, SendIcon } from "lucide-react";
import { useSiteProfile } from "../../features/site/useSiteProfile";
import type { SiteMenuView, SiteSocialLinkView } from "../../features/site/types";

function isExternalUrl(value: string): boolean {
  return /^(https?:)?\/\//.test(value) || value.startsWith("mailto:");
}

function renderMenuLink(
  item: SiteMenuView,
  className: string,
  style: React.CSSProperties,
) {
  if (isExternalUrl(item.href)) {
    return (
      <a
        key={`${item.label}-${item.href}`}
        href={item.href}
        target={item.href.startsWith("mailto:") ? undefined : "_blank"}
        rel={item.href.startsWith("mailto:") ? undefined : "noopener noreferrer"}
        className={className}
        style={style}
      >
        {item.label}
      </a>
    );
  }

  return (
    <Link
      key={`${item.label}-${item.href}`}
      to={item.href}
      className={className}
      style={style}
    >
      {item.label}
    </Link>
  );
}

function GitHubIcon() {
  return (
    <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" fill="currentColor">
      <path d="M12 2C6.48 2 2 6.58 2 12.26c0 4.52 2.87 8.35 6.84 9.7.5.1.68-.22.68-.49 0-.24-.01-.88-.01-1.73-2.78.62-3.37-1.38-3.37-1.38-.45-1.18-1.11-1.5-1.11-1.5-.91-.64.07-.63.07-.63 1 .07 1.53 1.06 1.53 1.06.9 1.57 2.35 1.12 2.92.86.09-.67.35-1.12.63-1.38-2.22-.26-4.55-1.14-4.55-5.07 0-1.12.39-2.04 1.03-2.76-.1-.26-.45-1.3.1-2.72 0 0 .84-.28 2.75 1.05A9.3 9.3 0 0 1 12 6.93c.85 0 1.7.12 2.5.34 1.9-1.33 2.74-1.05 2.74-1.05.55 1.42.2 2.46.1 2.72.64.72 1.03 1.64 1.03 2.76 0 3.94-2.34 4.81-4.57 5.06.36.32.68.94.68 1.9 0 1.38-.01 2.49-.01 2.83 0 .27.18.6.69.49A10.07 10.07 0 0 0 22 12.26C22 6.58 17.52 2 12 2Z" />
    </svg>
  );
}

function renderSocialIcon(link: SiteSocialLinkView) {
  const marker = `${link.icon} ${link.name} ${link.url}`.toLowerCase();
  if (marker.includes("github")) return <GitHubIcon />;
  if (marker.includes("mail")) return <MailIcon size={16} aria-hidden="true" />;
  if (link.icon && /^(https?:)?\/\//.test(link.icon)) {
    return <img src={link.icon} alt="" className="h-4 w-4 object-contain" />;
  }
  return <span className="text-xs">{link.name.slice(0, 1)}</span>;
}

export default function Footer() {
  const [email, setEmail] = useState("");
  const [subscribed, setSubscribed] = useState(false);
  const { profile, footerMenus } = useSiteProfile();
  const socialLinks = profile.socialLinks;

  const handleSubscribe = (event: React.FormEvent) => {
    event.preventDefault();
    if (email.trim()) {
      setSubscribed(true);
    }
  };

  return (
    <footer
      data-cmp="Footer"
      className="mt-24 border-t"
      style={{ background: "var(--section-bg)", borderColor: "var(--warm-border)" }}
    >
      <div className="mx-auto max-w-7xl px-8 py-16">
        <div data-slot="footer-main" className="flex flex-wrap justify-between gap-12">
          <div className="min-w-48 max-w-64 flex-1">
            <div
              style={{
                fontFamily: "var(--fontDisplay)",
                fontSize: "20px",
                color: "var(--ink)",
                marginBottom: "12px",
              }}
            >
              {profile.title || profile.ownerDisplayName}
            </div>
            {profile.footerDescription && (
              <p
                className="text-sm leading-relaxed"
                style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}
              >
                {profile.footerDescription}
              </p>
            )}
          </div>

          {footerMenus.length > 0 && (
            <div className="flex flex-wrap gap-16">
              {footerMenus.map((column) => {
                const links =
                  column.children.length > 0
                    ? column.children
                    : column.href && column.href !== "/"
                      ? [{ label: column.label, href: column.href, children: [] }]
                      : [];

                return (
                  <div key={`${column.label}-${column.href}`}>
                    <div
                      className="mb-4 text-xs font-medium uppercase tracking-widest"
                      style={{ color: "var(--muted-ink)" }}
                    >
                      {column.label}
                    </div>
                    <div className="flex flex-col gap-2.5">
                      {links.map((item) =>
                        renderMenuLink(item, "text-sm transition-colors hover:text-primary", {
                          color: "var(--muted-ink)",
                        }),
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          <div className="min-w-52">
            <div
              className="mb-4 text-xs font-medium uppercase tracking-widest"
              style={{ color: "var(--muted-ink)" }}
            >
              订阅更新
            </div>
            {subscribed ? (
              <div className="flex items-center gap-2 text-sm" style={{ color: "var(--olive)" }}>
                <span>✓</span>
                <span>感谢订阅</span>
              </div>
            ) : (
              <form onSubmit={handleSubscribe} className="flex gap-2">
                <input
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  placeholder="你的邮箱"
                  className="h-9 flex-1 border-b bg-transparent px-1 text-sm outline-none"
                  style={{
                    borderColor: "var(--warm-border)",
                    fontFamily: "var(--fontSans)",
                    color: "var(--ink)",
                    minWidth: 0,
                  }}
                />
                <button
                  type="submit"
                  className="flex h-9 w-9 shrink-0 items-center justify-center rounded-sm transition-colors hover:opacity-80"
                  style={{ background: "var(--ink)", color: "var(--warm-white)" }}
                >
                  <SendIcon size={14} />
                </button>
              </form>
            )}
          </div>
        </div>

        <div
          data-slot="footer-bottom"
          className="relative mt-12 flex flex-col gap-4 border-t pt-6 md:min-h-16 md:flex-row md:items-end md:justify-between"
          style={{ borderColor: "var(--warm-border)" }}
        >
          <div className="flex flex-col gap-2 md:min-w-0">
            {(profile.footerCopyright || profile.footerSlogan) && (
              <p className="text-xs" style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>
                {profile.footerCopyright}
                {profile.footerCopyright && profile.footerSlogan ? " · " : ""}
                {profile.footerSlogan}
              </p>
            )}
            {(profile.icpRecord || profile.policeRecord) && (
              <div
                className="flex flex-wrap gap-4 text-xs"
                style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}
              >
                {profile.icpRecord && <span>{profile.icpRecord}</span>}
                {profile.policeRecord && <span>{profile.policeRecord}</span>}
              </div>
            )}
          </div>

          {socialLinks.length > 0 && (
            <div
              data-slot="footer-social"
              className="flex items-center justify-center gap-3 md:absolute md:left-1/2 md:top-1/2 md:-translate-x-1/2 md:-translate-y-1/2"
            >
              {socialLinks.map((link) => (
                <a
                  key={`${link.name}-${link.url}`}
                  href={link.url}
                  target={link.url.startsWith("mailto:") ? undefined : "_blank"}
                  rel={link.url.startsWith("mailto:") ? undefined : "noopener noreferrer"}
                  className="inline-flex h-10 w-10 items-center justify-center rounded-sm border transition-colors hover:text-primary"
                  style={{ borderColor: "var(--warm-border)", color: "var(--muted-ink)" }}
                  aria-label={link.name}
                  title={link.name}
                >
                  {renderSocialIcon(link)}
                </a>
              ))}
            </div>
          )}

          <div className="flex gap-6 md:justify-end">
            <Link
              to="/stats"
              className="text-xs transition-colors hover:text-primary"
              style={{ color: "var(--muted-ink)" }}
            >
              站点统计
            </Link>
            <Link
              to="/guestbook"
              className="text-xs transition-colors hover:text-primary"
              style={{ color: "var(--muted-ink)" }}
            >
              留言
            </Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
