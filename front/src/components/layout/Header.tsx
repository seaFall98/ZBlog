import { useEffect, useRef, useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import {
  ArchiveIcon,
  BookOpenIcon,
  BookmarkIcon,
  BellIcon,
  ChevronDownIcon,
  ImageIcon,
  LogOutIcon,
  MenuIcon,
  SearchIcon,
  TagIcon,
  type LucideIcon,
  UserIcon,
  XIcon,
} from "lucide-react";
import { useAuth } from "../../features/auth/AuthProvider";
import { notificationApi } from "../../features/notifications/notificationApi";
import { useSiteProfile } from "../../features/site/useSiteProfile";
import type { SiteMenuView } from "../../features/site/types";

type NavChild = {
  label: string;
  href: string;
  icon: LucideIcon;
};

type NavItem = {
  label: string;
  href: string;
  children?: NavChild[];
};

function resolveChildIcon(item: SiteMenuView): LucideIcon {
  if (item.href.includes("/categories")) return BookmarkIcon;
  if (item.href.includes("/tags")) return TagIcon;
  if (item.href.includes("/archive")) return ArchiveIcon;
  if (item.href.includes("/gallery")) return ImageIcon;
  return BookOpenIcon;
}

function mapHeaderMenusToNavItems(menus: SiteMenuView[]): NavItem[] {
  return menus.map((item) => ({
    label: item.label,
    href: item.href,
    children: item.children.length > 0
      ? item.children.map((child) => ({
          label: child.label,
          href: child.href,
          icon: resolveChildIcon(child),
        }))
      : undefined,
  }));
}

function GitHubIcon() {
  return (
    <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" fill="currentColor">
      <path d="M12 2C6.48 2 2 6.58 2 12.26c0 4.52 2.87 8.35 6.84 9.7.5.1.68-.22.68-.49 0-.24-.01-.88-.01-1.73-2.78.62-3.37-1.38-3.37-1.38-.45-1.18-1.11-1.5-1.11-1.5-.91-.64.07-.63.07-.63 1 .07 1.53 1.06 1.53 1.06.9 1.57 2.35 1.12 2.92.86.09-.67.35-1.12.63-1.38-2.22-.26-4.55-1.14-4.55-5.07 0-1.12.39-2.04 1.03-2.76-.1-.26-.45-1.3.1-2.72 0 0 .84-.28 2.75 1.05A9.3 9.3 0 0 1 12 6.93c.85 0 1.7.12 2.5.34 1.9-1.33 2.74-1.05 2.74-1.05.55 1.42.2 2.46.1 2.72.64.72 1.03 1.64 1.03 2.76 0 3.94-2.34 4.81-4.57 5.06.36.32.68.94.68 1.9 0 1.38-.01 2.49-.01 2.83 0 .27.18.6.69.49A10.07 10.07 0 0 0 22 12.26C22 6.58 17.52 2 12 2Z" />
    </svg>
  );
}

type HeaderProps = {
  variant?: "default" | "guestbook";
};

export default function Header({ variant = "default" }: HeaderProps) {
  const { profile, headerMenus } = useSiteProfile();
  const { authenticated, user, logout } = useAuth();
  const navItems = mapHeaderMenusToNavItems(headerMenus);
  const isGuestbook = variant === "guestbook";
  const [mobileOpen, setMobileOpen] = useState(false);
  const [openMenu, setOpenMenu] = useState<string | null>(null);
  const [searchOpen, setSearchOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const searchRef = useRef<HTMLInputElement>(null);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (searchOpen && searchRef.current) {
      searchRef.current.focus();
    }
  }, [searchOpen]);

  useEffect(() => {
    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
  }, []);

  useEffect(() => {
    if (!authenticated) {
      setUnreadCount(0);
      return;
    }

    let alive = true;
    const refreshUnreadCount = () => {
      notificationApi
        .unreadCount()
        .then((count) => {
          if (alive) setUnreadCount(count);
        })
        .catch(() => {
          if (alive) setUnreadCount(0);
        });
    };

    refreshUnreadCount();
    window.addEventListener("focus", refreshUnreadCount);
    window.addEventListener("zblog:notifications-read", refreshUnreadCount);
    return () => {
      alive = false;
      window.removeEventListener("focus", refreshUnreadCount);
      window.removeEventListener("zblog:notifications-read", refreshUnreadCount);
    };
  }, [authenticated]);

  const handleMouseEnter = (label: string) => {
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    setOpenMenu(label);
  };

  const handleMouseLeave = () => {
    timeoutRef.current = setTimeout(() => setOpenMenu(null), 120);
  };

  const handleSearchSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    const keyword = (searchRef.current?.value ?? "").trim();
    if (!keyword) return;
    navigate(`/search?q=${encodeURIComponent(keyword)}`);
    setSearchOpen(false);
  };

  const handleLogout = async () => {
    await logout();
    navigate("/");
    setMobileOpen(false);
  };

  return (
    <header
      data-cmp="Header"
      className={`fixed top-0 left-0 right-0 z-50 h-16 ${isGuestbook ? "guestbook-header" : "bg-card border-b border-warm-border"}`}
      style={isGuestbook ? undefined : { borderBottomColor: "var(--warm-border)" }}
    >
      <div className="mx-auto flex h-full max-w-7xl items-center justify-between px-8">
        <Link
          to="/"
          className="shrink-0"
          style={{ fontFamily: "var(--fontDisplay)", fontSize: "18px", letterSpacing: "0.02em", color: "var(--ink)" }}
        >
          {profile.title}
        </Link>

        <nav className="hidden items-center gap-8 md:flex">
          {navItems.map((item) => (
            <div
              key={item.label}
              className="relative"
              onMouseEnter={item.children ? () => handleMouseEnter(item.label) : undefined}
              onMouseLeave={item.children ? handleMouseLeave : undefined}
            >
              <NavLink
                to={item.href}
                className={({ isActive }) =>
                  `flex items-center gap-0.5 text-sm transition-colors duration-200 ${isActive ? "text-primary" : "text-muted-foreground hover:text-foreground"}`
                }
                style={{ fontFamily: "var(--fontSans)", letterSpacing: "0.03em" }}
              >
                {item.label}
                {item.children && (
                  <ChevronDownIcon
                    size={13}
                    className={`transition-transform duration-200 ${openMenu === item.label ? "rotate-180" : ""}`}
                  />
                )}
              </NavLink>

              {item.children && (
                <div
                  className={`absolute top-full left-0 mt-3 min-w-40 rounded-sm border border-warm-border bg-card py-2 shadow-custom transition-all duration-200 ${
                    openMenu === item.label ? "pointer-events-auto translate-y-0 opacity-100" : "pointer-events-none -translate-y-1 opacity-0"
                  }`}
                  style={{ borderColor: "var(--warm-border)" }}
                  onMouseEnter={() => handleMouseEnter(item.label)}
                  onMouseLeave={handleMouseLeave}
                >
                  {item.children.map((child) => {
                    const ChildIcon = child.icon;
                    return (
                      <Link
                        key={child.href}
                        to={child.href}
                        className="flex items-center gap-2.5 px-4 py-2 text-sm text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground"
                        onClick={() => setOpenMenu(null)}
                      >
                        <ChildIcon size={13} />
                        {child.label}
                      </Link>
                    );
                  })}
                </div>
              )}
            </div>
          ))}
        </nav>

        <div className="hidden items-center gap-5 md:flex">
          <div className="flex items-center gap-2">
            {searchOpen && (
              <form onSubmit={handleSearchSubmit} className="flex items-center">
                <input
                  ref={searchRef}
                  type="text"
                  placeholder="搜索..."
                  className="h-7 w-48 border-b border-warm-border bg-transparent px-1 text-sm outline-none"
                  style={{ borderColor: "var(--warm-border)", fontFamily: "var(--fontSans)" }}
                  onBlur={() => {
                    if (timeoutRef.current) clearTimeout(timeoutRef.current);
                    timeoutRef.current = setTimeout(() => setSearchOpen(false), 200);
                  }}
                />
              </form>
            )}
            <button
              onClick={() => setSearchOpen((open) => !open)}
              className="text-muted-foreground transition-colors hover:text-foreground"
              aria-label="搜索"
            >
              <SearchIcon size={16} />
            </button>
          </div>
          <a
            href="https://github.com/seaFall98"
            target="_blank"
            rel="noopener noreferrer"
            className="text-muted-foreground transition-colors hover:text-foreground"
            aria-label="GitHub"
          >
            <GitHubIcon />
          </a>
          {authenticated && user ? (
            <div className="flex items-center gap-2">
              <Link
                to="/notifications"
                className="relative flex h-7 w-7 items-center justify-center rounded-full border text-muted-foreground transition-colors hover:text-foreground"
                style={{ borderColor: "var(--warm-border)" }}
                aria-label="通知"
              >
                <BellIcon size={15} />
                {unreadCount > 0 && (
                  <span
                    className="absolute -right-1 -top-1 flex h-4 min-w-4 items-center justify-center rounded-full px-1 text-[10px] leading-none"
                    style={{ background: "var(--ink)", color: "var(--warm-white)" }}
                  >
                    {unreadCount > 9 ? "9+" : unreadCount}
                  </span>
                )}
              </Link>
              <Link
                to="/profile"
                className="flex h-7 w-7 items-center justify-center overflow-hidden rounded-full border text-xs"
                style={{ borderColor: "var(--warm-border)", color: "var(--ink)" }}
                aria-label={user.nickname}
              >
                {user.avatar ? <img src={user.avatar} alt="" className="h-full w-full object-cover" /> : user.nickname.slice(0, 1)}
              </Link>
              <button
                type="button"
                onClick={handleLogout}
                className="text-muted-foreground transition-colors hover:text-foreground"
                aria-label="退出登录"
              >
                <LogOutIcon size={16} />
              </button>
            </div>
          ) : (
            <Link to="/login" className="text-muted-foreground transition-colors hover:text-foreground" aria-label="登录">
              <UserIcon size={16} />
            </Link>
          )}
        </div>

        <button
          className="text-muted-foreground hover:text-foreground md:hidden"
          onClick={() => setMobileOpen(true)}
          aria-label="打开菜单"
        >
          <MenuIcon size={20} />
        </button>
      </div>

      <div className={`fixed inset-0 z-50 transition-opacity duration-300 md:hidden ${mobileOpen ? "pointer-events-auto opacity-100" : "pointer-events-none opacity-0"}`}>
        <div className="absolute inset-0 bg-black/40" onClick={() => setMobileOpen(false)} />
        <div className={`absolute right-0 top-0 bottom-0 w-72 bg-card shadow-custom transition-transform duration-300 ${mobileOpen ? "translate-x-0" : "translate-x-full"}`}>
          <div className="flex items-center justify-between border-b border-warm-border p-6">
            <span style={{ fontFamily: "var(--fontDisplay)", fontSize: "16px" }}>{profile.title}</span>
            <button onClick={() => setMobileOpen(false)} className="text-muted-foreground">
              <XIcon size={20} />
            </button>
          </div>
          <nav className="flex flex-col gap-1 p-6">
            {navItems.map((item) => (
              <div key={item.label}>
                <Link
                  to={item.href}
                  className="block py-2 text-sm text-foreground transition-colors hover:text-primary"
                  onClick={() => setMobileOpen(false)}
                >
                  {item.label}
                </Link>
                {item.children && (
                  <div className="flex flex-col gap-0 pl-4">
                    {item.children.map((child) => (
                      <Link
                        key={child.href}
                        to={child.href}
                        className="block py-1.5 text-xs text-muted-foreground transition-colors hover:text-foreground"
                        onClick={() => setMobileOpen(false)}
                      >
                        {child.label}
                      </Link>
                    ))}
                  </div>
                )}
              </div>
            ))}
            <div className="mt-2 flex flex-col gap-2 border-t border-warm-border pt-4">
              <Link to="/search" className="block py-2 text-sm text-muted-foreground" onClick={() => setMobileOpen(false)}>
                搜索
              </Link>
              <a
                href="https://github.com/seaFall98"
                target="_blank"
                rel="noopener noreferrer"
                className="block py-2 text-sm text-muted-foreground"
                onClick={() => setMobileOpen(false)}
              >
                GitHub
              </a>
              {authenticated && user ? (
                <>
                  <Link to="/notifications" className="flex items-center justify-between py-2 text-sm text-muted-foreground" onClick={() => setMobileOpen(false)}>
                    <span>通知</span>
                    {unreadCount > 0 && (
                      <span className="rounded-full px-2 py-0.5 text-[11px]" style={{ background: "var(--ink)", color: "var(--warm-white)" }}>
                        {unreadCount > 99 ? "99+" : unreadCount}
                      </span>
                    )}
                  </Link>
                  <Link to="/profile" className="block py-2 text-sm text-muted-foreground" onClick={() => setMobileOpen(false)}>
                    个人资料
                  </Link>
                  <button type="button" className="block py-2 text-left text-sm text-muted-foreground" onClick={handleLogout}>
                    退出登录
                  </button>
                </>
              ) : (
                <Link to="/login" className="block py-2 text-sm text-muted-foreground" onClick={() => setMobileOpen(false)}>
                  登录
                </Link>
              )}
            </div>
          </nav>
        </div>
      </div>
    </header>
  );
}
