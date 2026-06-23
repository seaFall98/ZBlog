import { renderToStaticMarkup } from "react-dom/server";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import BlogList from "./BlogList";
import { usePosts } from "../features/blog/usePosts";
import { useCategories, useTags } from "../features/taxonomy/useTaxonomy";
import type { PostView } from "../features/blog/types";

vi.mock("@tanstack/react-query", () => ({
  useQuery: vi.fn(() => ({ data: undefined, isLoading: false })),
}));

vi.mock("../components/layout/PageLayout", () => ({
  default: ({ children }: { children: React.ReactNode }) => <main>{children}</main>,
}));

vi.mock("../features/blog/usePosts", () => ({
  usePosts: vi.fn(),
}));

vi.mock("../features/taxonomy/useTaxonomy", () => ({
  useCategories: vi.fn(),
  useTags: vi.fn(),
  findTaxonomyItemByRouteParam: vi.fn(() => undefined),
}));

vi.mock("../hooks/usePage", () => ({
  usePage: vi.fn(() => ({ page: 1, setPage: vi.fn() })),
  useNormalizePage: vi.fn(),
}));

const mockedUsePosts = vi.mocked(usePosts);
const mockedUseCategories = vi.mocked(useCategories);
const mockedUseTags = vi.mocked(useTags);

function postWith(overrides: Partial<PostView>): PostView {
  return {
    id: "1",
    slug: "top-post",
    title: "置顶文章",
    summary: "摘要",
    contentHtml: "<p>正文</p>",
    contentMarkdown: "",
    toc: [],
    category: { id: "life", slug: "life", name: "生活" },
    tags: [],
    publishedAt: "2026-06-20T08:00:00Z",
    coverUrl: "",
    readTime: 1,
    viewCount: 0,
    isTop: false,
    copyrightType: "ORIGINAL",
    sourceUrl: "",
    sourceTitle: "",
    copyrightLicense: "",
    featured: false,
    source: "api",
    ...overrides,
  };
}

describe("BlogList", () => {
  beforeEach(() => {
    mockedUseCategories.mockReturnValue({ items: [], loading: false, error: null, source: "api" });
    mockedUseTags.mockReturnValue({ items: [], loading: false, error: null, source: "api" });
  });

  it("marks top articles without changing list ordering", () => {
    mockedUsePosts.mockReturnValue({
      posts: [
        postWith({ id: "top", slug: "top-post", title: "重要文章", isTop: true, featured: true }),
        postWith({ id: "normal", slug: "normal-post", title: "普通文章" }),
      ],
      total: 2,
      page: 1,
      pageSize: 10,
      loading: false,
      error: null,
      source: "api",
    });

    const html = renderToStaticMarkup(
      <MemoryRouter initialEntries={["/blog"]}>
        <BlogList />
      </MemoryRouter>,
    );

    expect(html.indexOf("重要文章")).toBeLessThan(html.indexOf("普通文章"));
    expect(html).toContain('data-role="top-badge"');
    expect(html).toContain(">置顶<");
  });
});
