import { describe, expect, it, vi } from "vitest";
import { apiClient } from "../../lib/apiClient";
import { subscriptionApi } from "./subscriptionApi";

vi.mock("../../lib/apiClient", () => ({
  apiClient: {
    post: vi.fn(),
  },
}));

describe("subscriptionApi", () => {
  it("submits footer subscription emails to the real public subscribe API", async () => {
    vi.mocked(apiClient.post).mockResolvedValueOnce({ status: "PENDING" });

    await subscriptionApi.subscribe("reader@example.com");

    expect(apiClient.post).toHaveBeenCalledWith("/subscribe", {
      email: "reader@example.com",
    });
  });
});

