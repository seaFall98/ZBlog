import { BrowserRouter, Routes, Route } from "react-router-dom";
import { Toaster } from "sonner";

// Pages
import Index from "./pages/Index";
import BlogList from "./pages/BlogList";
import BlogDetail from "./pages/BlogDetail";
import Categories from "./pages/Categories";
import Tags from "./pages/Tags";
import Archive from "./pages/Archive";
import Search from "./pages/Search";
import Gallery from "./pages/Gallery";
import GalleryDetail from "./pages/GalleryDetail";
import Moments from "./pages/Moments";
import Guestbook from "./pages/Guestbook";
import Links from "./pages/Links";
import About from "./pages/About";
import Stats from "./pages/Stats";
import Login from "./pages/Login";

export default function App() {
  return (
    <BrowserRouter>
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
        <Route path="/blog/detail" element={<BlogDetail />} />
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
        <Route path="/gallery/detail" element={<Gallery />} />
        <Route path="/gallery/:slug" element={<GalleryDetail />} />
        <Route path="/moments" element={<Moments />} />
        <Route path="/guestbook" element={<Guestbook />} />
        <Route path="/links" element={<Links />} />
        <Route path="/about" element={<About />} />
        <Route path="/stats" element={<Stats />} />
        <Route path="/login" element={<Login />} />
      </Routes>
    </BrowserRouter>
  );
}
