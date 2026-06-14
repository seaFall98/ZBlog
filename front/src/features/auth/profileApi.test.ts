import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "../../lib/apiClient";
import { profileApi } from "./profileApi";

vi.mock("../../lib/apiClient", () => ({
  apiClient: {
    get: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    upload: vi.fn(),
  },
}));

describe("profileApi", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("updates the current user profile", async () => {
    vi.mocked(apiClient.put).mockResolvedValueOnce({});

    await profileApi.updateProfile({
      email: "reader@example.com",
      nickname: "Reader",
      bio: "quiet notes",
      badge: "读者",
    });

    expect(apiClient.put).toHaveBeenCalledWith("/user/profile", {
      email: "reader@example.com",
      nickname: "Reader",
      bio: "quiet notes",
      badge: "读者",
    });
  });

  it("uses the current user password endpoint", async () => {
    vi.mocked(apiClient.put).mockResolvedValueOnce(null);

    await profileApi.changePassword({
      old_password: "old-password",
      new_password: "new-password",
    });

    expect(apiClient.put).toHaveBeenCalledWith("/user/password", {
      old_password: "old-password",
      new_password: "new-password",
    });
  });

  it("uploads avatars with the reserved user avatar upload type", async () => {
    vi.mocked(apiClient.upload).mockResolvedValueOnce({});
    const file = new File(["avatar"], "avatar.png", { type: "image/png" });

    await profileApi.uploadAvatar(file);

    expect(apiClient.upload).toHaveBeenCalledWith("/upload", expect.any(FormData));
    const body = vi.mocked(apiClient.upload).mock.calls[0][1] as FormData;
    expect(body.get("file")).toBe(file);
    expect(body.get("type")).toBe("用户头像");
  });
});
