import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiEnvelopeError } from "./apiEnvelope";
import { ApiHttpError, createApiClient, setApiAuthTokenProvider } from "./apiClient";

describe("createApiClient", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    setApiAuthTokenProvider(null);
  });

  it("uses /api/v1 by default", async () => {
    const fetcher = vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: 0, message: "success", data: { ok: true } })));
    const client = createApiClient({ fetcher });

    await client.get("/articles", { page: 1, page_size: 10 });

    expect(fetcher).toHaveBeenCalledWith("/api/v1/articles?page=1&page_size=10", {
      headers: { Accept: "application/json" },
    });
  });

  it("uses explicit base url when provided", async () => {
    const fetcher = vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: 0, message: "success", data: [] })));
    const client = createApiClient({ baseUrl: "https://example.com/api/v1", fetcher });

    await client.get("/categories");

    expect(fetcher).toHaveBeenCalledWith("https://example.com/api/v1/categories", {
      headers: { Accept: "application/json" },
    });
  });

  it("adds bearer token when an auth provider is configured", async () => {
    const fetcher = vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: 0, message: "success", data: { ok: true } })));
    const client = createApiClient({ fetcher });
    setApiAuthTokenProvider(() => "phase-a-token");

    await client.get("/user/profile");

    expect(fetcher).toHaveBeenCalledWith("/api/v1/user/profile", {
      headers: { Accept: "application/json", Authorization: "Bearer phase-a-token" },
    });
  });

  it("uploads form data with auth headers but without forcing multipart content type", async () => {
    const fetcher = vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: 0, message: "success", data: { file_url: "/uploads/avatar.png" } })));
    const client = createApiClient({ fetcher });
    const formData = new FormData();
    formData.append("type", "用户头像");
    setApiAuthTokenProvider(() => "phase-b-token");

    await client.upload("/upload", formData);

    expect(fetcher).toHaveBeenCalledWith("/api/v1/upload", {
      method: "POST",
      headers: { Accept: "application/json", Authorization: "Bearer phase-b-token" },
      body: formData,
    });
  });

  it("throws ApiEnvelopeError with backend code and message when non-2xx response body is an error envelope", async () => {
    const fetcher = vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: 40401, message: "文章不存在" }), { status: 404 }));
    const client = createApiClient({ fetcher });

    const request = client.get("/articles/missing");

    await expect(request).rejects.toBeInstanceOf(ApiEnvelopeError);
    await expect(request).rejects.toMatchObject({
      name: "ApiEnvelopeError",
      code: 40401,
      message: "文章不存在",
    });
  });

  it("throws ApiHttpError when HTTP status is not ok and body is not an envelope", async () => {
    const fetcher = vi.fn().mockResolvedValue(new Response("not found", { status: 404 }));
    const client = createApiClient({ fetcher });

    await expect(client.get("/articles/missing")).rejects.toBeInstanceOf(ApiHttpError);
  });
});
