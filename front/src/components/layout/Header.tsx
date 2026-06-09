import { useEffect, useRef, useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import {
  ArchiveIcon,
  BookOpenIcon,
  BookmarkIcon,
  ChevronDownIcon,
  ImageIcon,
  MenuIcon,
  SearchIcon,
  TagIcon,
  UserIcon,
  XIcon,
  type LucideIcon,
} from "lucide-react";
import { useSiteProfile } from "../../features/site/useSiteProfile";

type NavItem = {
  label: string;
  href: string;
  icon?: LucideIcon;
  children?: NavItem[];
};

const fallbackNavItems: NavItem[] = [
  {
    label: "写作",
    href: "/blog",
    children: [
      { label: "文章列表", href: "/blog", icon: BookOpenIcon },
      { label: "分类", href: "/categories", icon: BookmarkIcon },
      { label: "标签", href: "/tags", icon: TagIcon },
      { label: "归档", href: "/archive", icon: ArchiveIcon },
    ],
  },
  {
    label: "影像",
    href: "/gallery",
    children: [
      { label: "相册图库", href: "/gallery", icon: ImageIcon },
    ],
  },
  { label: "瞬间", href: "/moments" },
  { label: "关于", href: "/about" },
];

function useHeaderNavItems(): { title: string; navItems: NavItem[] } {
  const { profile, menus } = useSiteProfile();
  const usableMenus = menus.filter((item) => item.href.startsWith("/")).slice(0, 6);
  return { title: profile.title || "寂静之书", navItems: usableMenus.length > 0 ? usableMenus : fallbackNavItems };
}

export default function Header() {
  const { title, navItems } = useHeaderNavItems();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [openMenu, setOpenMenu] = useState<string | null>(null);
  const [searchOpen, setSearchOpen] = useState(false);
  const searchRef = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (searchOpen && searchRef.current) searchRef.current.focus();
  }, [searchOpen]);

  const handleMouseEnter = (label: string) => {
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    setOpenMenu(label);
  };

  const handleMouseLeave = () => {
    timeoutRef.current = setTimeout(() => setOpenMenu(null), 120);
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const val = (searchRef.current?.value ?? "").trim();
    if (val) {
      navigate(`/search?q=${encodeURIComponent(val)}`);
      setSearchOpen(false);
    }
  };

  return (
    <header data-cmp="Header" className="fixed top-0 left-0 right-0 z-50 bg-card border-b border-warm-border h-16" style={{ borderBottomColor: "var(--warm-border)" }}>
      <div className="max-w-7xl mx-auto px-8 h-full flex items-center justify-between">
        <Link to="/" className="shrink-0" style={{ fontFamily: "var(--fontDisplay)", fontSize: "18px", letterSpacing: "0.02em", color: "var(--ink)" }}>
          {title}
        </Link>

        <nav className="hidden md:flex items-center gap-8">
          {navItems.map((item) => (
            <div key={item.label} className="relative" onMouseEnter={() => item.children ? handleMouseEnter(item.label) : undefined} onMouseLeave={item.children ? handleMouseLeave : undefined}>
              <NavLink to={item.href} className={({ isActive }) => `flex items-center gap-0.5 text-sm transition-colors duration-200 ${isActive ? "text-primary" : "text-muted-foreground hover:text-foreground"}`} style={{ fontFamily: "var(--fontSans)", letterSpacing: "0.03em" }}>
                {item.label}
                {item.children && <ChevronDownIcon size={13} className={`transition-transform duration-200 ${openMenu === item.label ? "rotate-180" : ""}`} />}
              </NavLink>

              {item.children && (
                <div className={`absolute top-full left-0 mt-3 bg-card border border-warm-border shadow-custom rounded-sm py-2 min-w-40 transition-all duration-200 ${openMenu === item.label ? "opacity-100 translate-y-0 pointer-events-auto" : "opacity-0 -translate-y-1 pointer-events-none"}`} style={{ borderColor: "var(--warm-border)" }} onMouseEnter={() => handleMouseEnter(item.label)} onMouseLeave={handleMouseLeave}>
                  {item.children.map((child) => {
                    const ChildIcon = child.icon;
                    return (
                      <Link key={child.href} to={child.href} className="flex items-center gap-2.5 px-4 py-2 text-sm text-muted-foreground hover:text-foreground hover:bg-secondary transition-colors" onClick={() => setOpenMenu(null)}>
                        {ChildIcon && <ChildIcon size={13} />}
                        {child.label}
                      </Link>
                    );
                  })}
                </div>
              )}
            </div>
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
              <div key={item.label}>
                <Link to={item.href} className="block py-2 text-sm text-foreground hover:text-primary transition-colors" onClick={() => setMobileOpen(false)}>{item.label}</Link>
                {item.children && <div className="pl-4 flex flex-col gap-0">{item.children.map((child) => <Link key={child.href} to={child.href} className="block py-1.5 text-xs text-muted-foreground hover:text-foreground transition-colors" onClick={() => setMobileOpen(false)}>{child.label}</Link>)}</div>}
              </div>
            ))}
            <div className="pt-4 border-t border-warm-border mt-2 flex flex-col gap-2">
              <Link to="/search" className="block py-2 text-sm text-muted-foreground" onClick={() => setMobileOpen(false)}>搜索</Link>
              <Link to="/login" className="block py-2 text-sm text-muted-foreground" onClick={() => setMobileOpen(false)}>登录</Link>
            </div>
          </nav>
        </div>
      </div>
    </header>
  );
}
