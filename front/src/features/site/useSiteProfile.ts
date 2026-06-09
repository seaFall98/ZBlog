import { useEffect, useState } from "react";
import { fetchMenus, fetchSiteProfile } from "./siteApi";
import type { SiteMenuView, SiteProfileView } from "./types";

export const defaultSiteProfile: SiteProfileView = {
  title: "寂静之书",
  subtitle: "记录平凡生活里的光与影",
  aboutIntro: "一个喜欢在平凡生活里寻找微小美好的人。",
  email: "hello@quietbook.me",
  avatarUrl: "https://images.unsplash.com/photo-1529665253569-6d01c0eaf7b6?w=600&q=80",
};

export function useSiteProfile() {
  const [profile, setProfile] = useState(defaultSiteProfile);
  const [menus, setMenus] = useState<SiteMenuView[]>([]);

  useEffect(() => {
    let active = true;
    void Promise.allSettled([fetchSiteProfile(), fetchMenus()]).then(([profileResult, menusResult]) => {
      if (!active) return;
      if (profileResult.status === "fulfilled") setProfile((current) => ({ ...current, ...profileResult.value }));
      if (menusResult.status === "fulfilled") setMenus(menusResult.value);
    });
    return () => { active = false; };
  }, []);

  return { profile, menus };
}
