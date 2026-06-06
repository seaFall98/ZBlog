import { useLocation } from "react-router-dom";
import Header from "./Header";
import Footer from "./Footer";
import { Toaster } from "sonner";

interface PageLayoutProps {
  children: React.ReactNode;
  noFooter?: boolean;
}

export default function PageLayout({ children, noFooter = false }: PageLayoutProps) {
  const { pathname } = useLocation();

  return (
    <div data-cmp="PageLayout" className="min-h-screen flex flex-col" style={{ background: "var(--ivory)" }}>
      <Toaster position="top-center" richColors />
      <Header />
      <main
        key={pathname}
        className="flex-1 pt-16 page-fade-in"
      >
        {children}
      </main>
      {!noFooter && <Footer />}
    </div>
  );
}
