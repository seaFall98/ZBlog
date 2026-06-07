import { describe, expect, it } from "vitest";
import { ApiEnvelopeError, parseApiEnvelope } from "./apiEnvelope";

describe("parseApiEnvelope", () => {
  it("returns data when code is 0", () => {
    expect(parseApiEnvelope<{ title: string }>({ code: 0, message: "success", data: { title: "寂静之书" } })).toEqual({ title: "寂静之书" });
  });

  it("allows omitted data for successful empty responses", () => {
    expect(parseApiEnvelope<undefined>({ code: 0, message: "success" })).toBeUndefined();
  });

  it("throws ApiEnvelopeError for non-zero code", () => {
    expect(() => parseApiEnvelope({ code: 40001, message: "文章不存在" })).toThrow(ApiEnvelopeError);
  });

  it("throws for non-envelope values", () => {
    expect(() => parseApiEnvelope({ ok: true })).toThrow("Invalid API response envelope");
  });
});
