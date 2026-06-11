import { describe, expect, it } from "vitest";
import {
  DEFAULT_MENU_TYPE,
  getMenuTypeLabel,
  MENU_TYPE_OPTIONS,
} from "./menuSchema";

describe("menuSchema", () => {
  it("exposes only the v2 menu types", () => {
    expect(MENU_TYPE_OPTIONS.map((item) => item.value)).toEqual([
      "header_navigation",
      "footer_navigation",
    ]);
    expect(DEFAULT_MENU_TYPE).toBe("header_navigation");
  });

  it("returns user-facing labels for the v2 menu types", () => {
    expect(getMenuTypeLabel("header_navigation")).toBe("顶部导航菜单");
    expect(getMenuTypeLabel("footer_navigation")).toBe("页脚导航菜单");
  });
});
