import { useEffect } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { Toaster } from "sonner";
import { blogApi } from "./features/blog/blogApi";
import { fetchAlbum } from "./features/gallery/galleryApi";
import { AuthProvider } from "./features/auth/AuthProvider";
import { SiteProfileProvider } from "./features/site/useSiteProfile";
import About from "./pages/About";
import Archive from "./pages/Archive";
import BlogDetail from "./pages/BlogDetail";
import BlogList from "./pages/BlogList";
import Categories from "./pages/Categories";
import Cookies from "./pages/Cookies";
import Copyright from "./pages/Copyright";
import Gallery from "./pages/Gallery";
import GalleryDetail from "./pages/GalleryDetail";
import Guestbook from "./pages/Guestbook";
import Index from "./pages/Index";
import Links from "./pages/Links";
import Login from "./pages/Login";
import Moments from "./pages/Moments";
import NotFound from "./pages/NotFound";
import Notifications from "./pages/Notifications";
import Privacy from "./pages/Privacy";
import Profile from "./pages/Profile";
import Search from "./pages/Search";
import Stats from "./pages/Stats";
import Tags from "./pages/Tags";
import ProtectedRoute from "./features/auth/ProtectedRoute";

function LinkPrefetch() {
  const qc = useQueryClient();

  useEffect(() => {
    const prefetchPost = (slug: string) => {
      qc.prefetchQuery({
        queryKey: ["post", slug],
        queryFn: () => blogApi.getPost(slug),
      });
    };
    const prefetchAlbum = (slug: string) => {
      qc.prefetchQuery({
        queryKey: ["album", slug],
        queryFn: () => fetchAlbum(slug),
      });
    };

    const onHover = (e: MouseEvent) => {
      const a = (e.target as Element).closest<HTMLAnchorElement>("a[href]");
      if (!a) return;
      const href = a.getAttribute("href") || "";
      const postMatch = href.match(/^\/posts\/([^/?#]+)/);
      if (postMatch) {
        prefetchPost(postMatch[1]);
        return;
      }
      const galleryMatch = href.match(/^\/gallery\/([^/?#]+)/);
      if (galleryMatch) {
        prefetchAlbum(galleryMatch[1]);
      }
    };

    document.addEventListener("mouseover", onHover, { capture: true, passive: true });
    return () => document.removeEventListener("mouseover", onHover, { capture: true });
  }, [qc]);

  return null;
}

export default function App() {
  return (
    <SiteProfileProvider>
      <AuthProvider>
        <BrowserRouter>
          <LinkPrefetch />
          <Toaster
            position="top-right"
            toastOptions={{
              style: {
                fontFamily: "var(--fontSans)",
                fontSize: "13px",
                background: "var(--warm-white)",
                color: "var(--ink)",
                border: "1px solid var(--warm-border)",
              },
            }}
          />

          <Routes>
            <Route path="/" element={<Index />} />
            <Route path="/blog" element={<BlogList />} />
            <Route path="/blog/detail" element={<NotFound />} />
            <Route path="/posts/:slug" element={<BlogDetail />} />
            <Route path="/blog/:slug" element={<BlogDetail />} />
            <Route path="/category/:slug" element={<BlogList />} />
            <Route path="/tag/:slug" element={<BlogList />} />
            <Route path="/categories" element={<Categories />} />
            <Route path="/tags" element={<Tags />} />
            <Route path="/archive" element={<Archive />} />
            <Route path="/archive/:year/:month" element={<Archive />} />
            <Route path="/search" element={<Search />} />
            <Route path="/gallery" element={<Gallery />} />
            <Route path="/gallery/detail" element={<NotFound />} />
            <Route path="/gallery/:slug" element={<GalleryDetail />} />
            <Route path="/moments" element={<Moments />} />
            <Route path="/guestbook" element={<Guestbook />} />
            <Route path="/message" element={<Navigate to="/guestbook" replace />} />
            <Route path="/links" element={<Links />} />
            <Route path="/about" element={<About />} />
            <Route path="/privacy" element={<Privacy />} />
            <Route path="/cookies" element={<Cookies />} />
            <Route path="/copyright" element={<Copyright />} />
            <Route path="/stats" element={<Stats />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Login />} />
            <Route path="/forgot-password" element={<Login />} />
            <Route
              path="/profile"
              element={
                <ProtectedRoute>
                  <Profile />
                </ProtectedRoute>
              }
            />
            <Route
              path="/notifications"
              element={
                <ProtectedRoute>
                  <Notifications />
                </ProtectedRoute>
              }
            />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </SiteProfileProvider>
  );
}
