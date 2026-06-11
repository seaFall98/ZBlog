import { describe, expect, it } from "vitest";
import { mapComment } from "./commentApi";

describe("mapComment", () => {
  it("maps nested public comments", () => {
    expect(mapComment({
      id: 1,
      nickname: "阿秋",
      content: "你好",
      created_at: "2024-10-20",
      replies: [{ id: 2, parent_id: 1, nickname: "海", content: "欢迎" }],
    })).toMatchObject({
      id: "1",
      nickname: "阿秋",
      content: "你好",
      createdAt: "2024-10-20",
      replies: [{ id: "2", parentId: "1", nickname: "海", content: "欢迎" }],
    });
  });

  it("maps backend user object fields", () => {
    expect(mapComment({
      id: 3,
      content: "来自后端结构",
      user: { nickname: "山月", website: "https://example.com", avatar: "/uploads/a.png" },
    })).toMatchObject({
      id: "3",
      nickname: "山月",
      website: "https://example.com",
      avatar: "/uploads/a.png",
    });
  });

  it("drops empty comments", () => {
    expect(mapComment({ id: 1, nickname: "空", content: "  " })).toBeNull();
  });
});
