import { afterEach, describe, expect, it, vi } from "vitest";
import { authApi } from "./authApi";
import { apiClient } from "../../lib/apiClient";

vi.mock("../../lib/apiClient", () => ({
  apiClient: {
    post: vi.fn(),
  },
}));

describe("authApi", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("posts login credentials to the auth login endpoint", async () => {
    vi.mocked(apiClient.post).mockResolvedValueOnce({});

    await authApi.login({ email: "reader@example.com", password: "reader123456" });

    expect(apiClient.post).toHaveBeenCalledWith("/auth/login", {
      email: "reader@example.com",
      password: "reader123456",
    });
  });

  it("posts registration payload to the auth register endpoint", async () => {
    vi.mocked(apiClient.post).mockResolvedValueOnce({});

    await authApi.register({
      email: "reader@example.com",
      nickname: "Reader",
      password: "reader123456",
    });

    expect(apiClient.post).toHaveBeenCalledWith("/auth/register", {
      email: "reader@example.com",
      nickname: "Reader",
      password: "reader123456",
    });
  });

  it("uses the V1-style reset code endpoints", async () => {
    vi.mocked(apiClient.post).mockResolvedValue({});

    await authApi.forgotPassword({ email: "reader@example.com" });
    await authApi.resetPassword({
      email: "reader@example.com",
      code: "123456",
      password: "newReader123456",
    });

    expect(apiClient.post).toHaveBeenNthCalledWith(1, "/auth/forgot-password", {
      email: "reader@example.com",
    });
    expect(apiClient.post).toHaveBeenNthCalledWith(2, "/auth/reset-password", {
      email: "reader@example.com",
      code: "123456",
      password: "newReader123456",
    });
  });
});
