import Header from "./Header";
import Footer from "./Footer";

interface PageLayoutProps {
  children: React.ReactNode;
  noFooter?: boolean;
  headerVariant?: "default" | "guestbook";
  noMainTopPadding?: boolean;
}

export default function PageLayout({ children, noFooter = false, headerVariant = "default", noMainTopPadding = false }: PageLayoutProps) {
  return (
    <div data-cmp="PageLayout" className="min-h-screen flex flex-col" style={{ background: "var(--ivory)" }}>
      <Header variant={headerVariant} />
      <main
        className={`flex-1 page-fade-in ${noMainTopPadding ? "" : "pt-16"}`}
      >
        {children}
      </main>
      {!noFooter && <Footer />}
    </div>
  );
}
