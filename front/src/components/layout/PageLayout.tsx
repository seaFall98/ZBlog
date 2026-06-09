import { useLocation } from "react-router-dom";
import Header from "./Header";
import Footer from "./Footer";
import { Toaster } from "sonner";

interface PageLayoutProps {
  children: React.ReactNode;
  noFooter?: boolean;
  headerVariant?: "default" | "guestbook";
  noMainTopPadding?: boolean;
}

export default function PageLayout({ children, noFooter = false, headerVariant = "default", noMainTopPadding = false }: PageLayoutProps) {
  const { pathname } = useLocation();

  return (
    <div data-cmp="PageLayout" className="min-h-screen flex flex-col" style={{ background: "var(--ivory)" }}>
      <Toaster position="top-center" richColors />
      <Header variant={headerVariant} />
      <main
        key={pathname}
        className={`flex-1 page-fade-in ${noMainTopPadding ? "" : "pt-16"}`}
      >
        {children}
      </main>
      {!noFooter && <Footer />}
    </div>
  );
}
