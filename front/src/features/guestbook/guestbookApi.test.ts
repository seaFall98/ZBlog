import { describe, expect, it } from "vitest";
import { mapGuestbookMessage, mapGuestbookSubmitResult } from "./guestbookApi";

describe("mapGuestbookMessage", () => {
  it("maps public message fields", () => {
    expect(mapGuestbookMessage({ id: 1, nickname: "云", content: "你好", created_at: "2024-10-20" })).toMatchObject({
      id: "1",
      name: "云",
      content: "你好",
      date: "2024-10-20",
    });
  });
});

describe("mapGuestbookSubmitResult", () => {
  it("keeps backend status and message", () => {
    expect(mapGuestbookSubmitResult({ id: 9, status: "pending", message: "留言需要审核" })).toEqual({
      id: "9",
      status: "pending",
      message: "留言需要审核",
    });
  });
});
