import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "../../lib/apiClient";
import { mapNotification, notificationApi } from "./notificationApi";

vi.mock("../../lib/apiClient", () => ({
  apiClient: {
    get: vi.fn(),
    put: vi.fn(),
  },
}));

describe("notificationApi", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("maps backend notification fields", () => {
    expect(
      mapNotification({
        id: 7,
        type: "comment_reply",
        type_text: "评论回复",
        title: "Reader 回复了你",
        content: "hello",
        link: "/posts/demo#comment-9",
        target_comment_id: 9,
        is_read: false,
        created_at: "2026-06-14 10:00:00",
      }),
    ).toMatchObject({
      id: "7",
      type: "comment_reply",
      typeText: "评论回复",
      link: "/posts/demo#comment-9",
      targetCommentId: "9",
      isRead: false,
    });
  });

  it("lists notifications with unread filter", async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({
      list: [{ id: 1, title: "A" }],
      total: 1,
      page: 2,
      page_size: 10,
      unread_count: 1,
    });

    const page = await notificationApi.list({ page: 2, pageSize: 10, unreadOnly: true });

    expect(apiClient.get).toHaveBeenCalledWith("/notifications", {
      page: 2,
      page_size: 10,
      unread_only: true,
    });
    expect(page.list[0].id).toBe("1");
    expect(page.unreadCount).toBe(1);
  });

  it("uses recipient scoped read endpoints", async () => {
    vi.mocked(apiClient.put).mockResolvedValueOnce({});

    await notificationApi.markRead("9");

    expect(apiClient.put).toHaveBeenCalledWith("/notifications/9/read");
  });
});
