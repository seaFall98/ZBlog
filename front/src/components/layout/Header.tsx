import { useEffect, useRef, useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import {
  MenuIcon,
  SearchIcon,
  UserIcon,
  XIcon,
} from "lucide-react";
import { useSiteProfile } from "../../features/site/useSiteProfile";

type NavItem = {
  label: string;
  href: string;
};

const requiredTopNavItems: NavItem[] = [
  { label: "写作", href: "/blog" },
  { label: "相册", href: "/gallery" },
  { label: "瞬间", href: "/moments" },
  { label: "关于", href: "/about" },
];

function constrainTopNavItems(_menus: NavItem[]): NavItem[] {
  return requiredTopNavItems;
}

function GitHubIcon() {
  return (
    <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" fill="currentColor">
      <path d="M12 2C6.48 2 2 6.58 2 12.26c0 4.52 2.87 8.35 6.84 9.7.5.1.68-.22.68-.49 0-.24-.01-.88-.01-1.73-2.78.62-3.37-1.38-3.37-1.38-.45-1.18-1.11-1.5-1.11-1.5-.91-.64.07-.63.07-.63 1 .07 1.53 1.06 1.53 1.06.9 1.57 2.35 1.12 2.92.86.09-.67.35-1.12.63-1.38-2.22-.26-4.55-1.14-4.55-5.07 0-1.12.39-2.04 1.03-2.76-.1-.26-.45-1.3.1-2.72 0 0 .84-.28 2.75 1.05A9.3 9.3 0 0 1 12 6.93c.85 0 1.7.12 2.5.34 1.9-1.33 2.74-1.05 2.74-1.05.55 1.42.2 2.46.1 2.72.64.72 1.03 1.64 1.03 2.76 0 3.94-2.34 4.81-4.57 5.06.36.32.68.94.68 1.9 0 1.38-.01 2.49-.01 2.83 0 .27.18.6.69.49A10.07 10.07 0 0 0 22 12.26C22 6.58 17.52 2 12 2Z" />
    </svg>
  );
}

function useHeaderNavItems(): { title: string; navItems: NavItem[] } {
  const { profile, menus } = useSiteProfile();
  return { title: profile.title || "寂静之书", navItems: constrainTopNavItems(menus) };
}

type HeaderProps = {
  variant?: "default" | "guestbook";
};

export default function Header({ variant = "default" }: HeaderProps) {
  const { title, navItems } = useHeaderNavItems();
  const isGuestbook = variant === "guestbook";
  const [mobileOpen, setMobileOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const searchRef = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (searchOpen && searchRef.current) searchRef.current.focus();
  }, [searchOpen]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const val = (searchRef.current?.value ?? "").trim();
    if (val) {
      navigate(`/search?q=${encodeURIComponent(val)}`);
      setSearchOpen(false);
    }
  };

  return (
    <header
      data-cmp="Header"
      className={`fixed top-0 left-0 right-0 z-50 h-16 ${isGuestbook ? "guestbook-header" : "bg-card border-b border-warm-border"}`}
      style={isGuestbook ? undefined : { borderBottomColor: "var(--warm-border)" }}
    >
      <div className="max-w-7xl mx-auto px-8 h-full flex items-center justify-between">
        <Link to="/" className="shrink-0" style={{ fontFamily: "var(--fontDisplay)", fontSize: "18px", letterSpacing: "0.02em", color: "var(--ink)" }}>
          {title}
        </Link>

        <nav className="hidden md:flex items-center gap-8">
          {navItems.map((item) => (
            <NavLink key={item.label} to={item.href} className={({ isActive }) => `flex items-center gap-0.5 text-sm transition-colors duration-200 ${isActive ? "text-primary" : "text-muted-foreground hover:text-foreground"}`} style={{ fontFamily: "var(--fontSans)", letterSpacing: "0.03em" }}>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="hidden md:flex items-center gap-5">
          <div className="flex items-center gap-2">
            {searchOpen && (
              <form onSubmit={handleSearchSubmit} className="flex items-center">
                <input ref={searchRef} type="text" placeholder="搜索..." className="w-48 h-7 text-sm bg-transparent border-b border-warm-border outline-none px-1" style={{ borderColor: "var(--warm-border)", fontFamily: "var(--fontSans)" }} onBlur={() => setTimeout(() => setSearchOpen(false), 200)} />
              </form>
            )}
            <button onClick={() => setSearchOpen(!searchOpen)} className="text-muted-foreground hover:text-foreground transition-colors" aria-label="搜索"><SearchIcon size={16} /></button>
          </div>
          <a href="https://github.com/seaFall98" target="_blank" rel="noopener noreferrer" className="text-muted-foreground hover:text-foreground transition-colors" aria-label="GitHub"><GitHubIcon /></a>
          <Link to="/login" className="text-muted-foreground hover:text-foreground transition-colors" aria-label="登录"><UserIcon size={16} /></Link>
        </div>

        <button className="md:hidden text-muted-foreground hover:text-foreground" onClick={() => setMobileOpen(true)} aria-label="打开菜单"><MenuIcon size={20} /></button>
      </div>

      <div className={`fixed inset-0 z-50 md:hidden transition-opacity duration-300 ${mobileOpen ? "opacity-100 pointer-events-auto" : "opacity-0 pointer-events-none"}`}>
        <div className="absolute inset-0 bg-black/40" onClick={() => setMobileOpen(false)} />
        <div className={`absolute right-0 top-0 bottom-0 w-72 bg-card shadow-custom transition-transform duration-300 ${mobileOpen ? "translate-x-0" : "translate-x-full"}`}>
          <div className="flex items-center justify-between p-6 border-b border-warm-border">
            <span style={{ fontFamily: "var(--fontDisplay)", fontSize: "16px" }}>{title}</span>
            <button onClick={() => setMobileOpen(false)} className="text-muted-foreground"><XIcon size={20} /></button>
          </div>
          <nav className="p-6 flex flex-col gap-1">
            {navItems.map((item) => (
              <Link key={item.label} to={item.href} className="block py-2 text-sm text-foreground hover:text-primary transition-colors" onClick={() => setMobileOpen(false)}>{item.label}</Link>
            ))}
            <div className="pt-4 border-t border-warm-border mt-2 flex flex-col gap-2">
              <Link to="/search" className="block py-2 text-sm text-muted-foreground" onClick={() => setMobileOpen(false)}>搜索</Link>
              <a href="https://github.com/seaFall98" target="_blank" rel="noopener noreferrer" className="block py-2 text-sm text-muted-foreground" onClick={() => setMobileOpen(false)}>GitHub</a>
              <Link to="/login" className="block py-2 text-sm text-muted-foreground" onClick={() => setMobileOpen(false)}>登录</Link>
            </div>
          </nav>
        </div>
      </div>
    </header>
  );
}
