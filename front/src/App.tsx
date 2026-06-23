import { lazy, Suspense, useEffect } from "react";
import { BrowserRouter, Navigate, Route, Routes, useLocation, useNavigationType } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { Toaster } from "sonner";
import { blogApi } from "./features/blog/blogApi";
import { fetchAlbum } from "./features/gallery/galleryApi";
import { AuthProvider } from "./features/auth/AuthProvider";
import { SiteProfileProvider } from "./features/site/useSiteProfile";
import { usePageViewCollector } from "./features/stats/usePageViewCollector";
import ProtectedRoute from "./features/auth/ProtectedRoute";
import { RouteSeoDefaults } from "./features/seo/SeoHead";

const About = lazy(() => import("./pages/About"));
const Archive = lazy(() => import("./pages/Archive"));
const BlogDetail = lazy(() => import("./pages/BlogDetail"));
const BlogList = lazy(() => import("./pages/BlogList"));
const Categories = lazy(() => import("./pages/Categories"));
const Cookies = lazy(() => import("./pages/Cookies"));
const Copyright = lazy(() => import("./pages/Copyright"));
const Gallery = lazy(() => import("./pages/Gallery"));
const GalleryDetail = lazy(() => import("./pages/GalleryDetail"));
const Feedback = lazy(() => import("./pages/Feedback"));
const Guestbook = lazy(() => import("./pages/Guestbook"));
const Index = lazy(() => import("./pages/Index"));
const Links = lazy(() => import("./pages/Links"));
const Login = lazy(() => import("./pages/Login"));
const Moments = lazy(() => import("./pages/Moments"));
const MyFeedback = lazy(() => import("./pages/MyFeedback"));
const NotFound = lazy(() => import("./pages/NotFound"));
const Notifications = lazy(() => import("./pages/Notifications"));
const Privacy = lazy(() => import("./pages/Privacy"));
const Profile = lazy(() => import("./pages/Profile"));
const Search = lazy(() => import("./pages/Search"));
const Stats = lazy(() => import("./pages/Stats"));
const Tags = lazy(() => import("./pages/Tags"));

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

function ScrollToTop() {
  const location = useLocation();
  const navigationType = useNavigationType();

  useEffect(() => {
    if (navigationType === "POP" || location.hash) return;
    const params = new URLSearchParams(location.search);
    if (params.has("commentId")) return;
    window.scrollTo({ top: 0, left: 0 });
  }, [location.hash, location.pathname, location.search, navigationType]);

  return null;
}

function PublicPageViewCollector() {
  usePageViewCollector();
  return null;
}

export default function App() {
  return (
    <SiteProfileProvider>
      <AuthProvider>
        <BrowserRouter>
          <LinkPrefetch />
          <ScrollToTop />
          <PublicPageViewCollector />
          <RouteSeoDefaults />
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

          <Suspense fallback={null}>
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
              <Route path="/feedback" element={<Feedback />} />
              <Route path="/feedback/mine" element={<MyFeedback />} />
              <Route path="/stats" element={<Stats />} />
              <Route path="/statistics" element={<Navigate to="/stats" replace />} />
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
          </Suspense>
        </BrowserRouter>
      </AuthProvider>
    </SiteProfileProvider>
  );
}
