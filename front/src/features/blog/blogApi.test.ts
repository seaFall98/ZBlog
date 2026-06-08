import { describe, expect, it, vi } from "vitest";
import { createBlogApi } from "./blogApi";

function jsonResponse(data: unknown): Response {
  return new Response(JSON.stringify({ code: 0, message: "success", data }));
}

describe("blogApi", () => {
  it("fetches article list with category params", async () => {
    const fetcher = vi.fn().mockResolvedValue(
      jsonResponse({
        list: [
          {
            id: 1,
            slug: "life-post",
            title: "生活文章",
            content_html: "<p>生活</p>",
          },
        ],
        total: 1,
        page: 1,
        page_size: 10,
      }),
    );
    const api = createBlogApi({ fetcher });

    const result = await api.listPosts({ page: 1, pageSize: 10, category: "life" });

    expect(fetcher).toHaveBeenCalledWith("/api/v1/articles?page=1&page_size=10&category=life", {
      headers: { Accept: "application/json" },
    });
    expect(result.posts[0]).toMatchObject({ slug: "life-post", title: "生活文章" });
    expect(result.total).toBe(1);
  });

  it("fetches an article detail by encoded slug", async () => {
    const fetcher = vi.fn().mockResolvedValue(
      jsonResponse({
        id: "cn",
        slug: "秋日",
        title: "秋日",
        content_html: "<p>秋日</p>",
      }),
    );
    const api = createBlogApi({ fetcher });

    const post = await api.getPost("秋日");

    expect(fetcher).toHaveBeenCalledWith("/api/v1/articles/%E7%A7%8B%E6%97%A5", {
      headers: { Accept: "application/json" },
    });
    expect(post.slug).toBe("秋日");
  });

  it("searches articles with keyword and pagination params", async () => {
    const fetcher = vi.fn().mockResolvedValue(
      jsonResponse({
        list: [
          {
            id: "search",
            slug: "autumn-light",
            title: "秋日午后",
            content_html: "<p>光</p>",
          },
        ],
        total: 1,
      }),
    );
    const api = createBlogApi({ fetcher });

    const result = await api.searchPosts({ keyword: "秋日 光", page: 1, pageSize: 50 });

    expect(fetcher).toHaveBeenCalledWith("/api/v1/articles/search?keyword=%E7%A7%8B%E6%97%A5+%E5%85%89&page=1&page_size=50", {
      headers: { Accept: "application/json" },
    });
    expect(result.posts).toHaveLength(1);
  });
});
