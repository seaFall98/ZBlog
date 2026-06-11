import { useEffect, useState } from "react";
import { fetchMenus, fetchSiteProfile } from "./siteApi";
import type { SiteMenuView, SiteProfileView } from "./types";

export const defaultSiteProfile: SiteProfileView = {
  title: "寂静之书",
  subtitle: "记录平凡生活里的光与影",
  aboutIntro: "一个喜欢在平凡生活里寻找微小美好的人。",
  email: "hello@quietbook.me",
  avatarUrl: "https://images.unsplash.com/photo-1529665253569-6d01c0eaf7b6?w=600&q=80",
  faviconUrl: "",
  established: "2024",
  heroEyebrow: "个人出版物",
  heroTitle: "以文字作舟，\n渡光阴\n之河",
  heroSlogan: "以文字作舟，渡光阴之河",
  footerDescription: "记录平凡生活里的光与影，写作是一种安静的对话。",
  footerCopyright: "",
  footerSlogan: "以文字作舟，渡光阴之河",
  backgroundImage: "",
  barrageBackgroundImage: "",
  messageContent: "",
  aboutDescribe: "一个喜欢在平凡生活里寻找微小美好的人。",
  aboutDescribeTips: "",
  aboutExhibition: "",
  aboutProfile: "",
  aboutPersonality: "",
  aboutMottoMain: "",
  aboutMottoSub: "",
  aboutStory: "",
};

function applyDocumentProfile(profile: SiteProfileView) {
  if (typeof document === "undefined") return;
  document.title = profile.title || defaultSiteProfile.title;

  const faviconHref = profile.faviconUrl || defaultSiteProfile.faviconUrl;
  if (!faviconHref) return;

  let favicon = document.querySelector<HTMLLinkElement>('link[rel~="icon"]');
  if (!favicon) {
    favicon = document.createElement("link");
    favicon.rel = "icon";
    document.head.appendChild(favicon);
  }
  favicon.href = faviconHref;
}

function mergeProfile(current: SiteProfileView, incoming: SiteProfileView): SiteProfileView {
  const nextProfile = { ...current };
  (Object.keys(incoming) as Array<keyof SiteProfileView>).forEach((key) => {
    if (incoming[key]) nextProfile[key] = incoming[key];
  });
  return nextProfile;
}

export function useSiteProfile() {
  const [profile, setProfile] = useState(defaultSiteProfile);
  const [menus, setMenus] = useState<SiteMenuView[]>([]);

  useEffect(() => {
    let active = true;
    void Promise.allSettled([fetchSiteProfile(), fetchMenus()]).then(([profileResult, menusResult]) => {
      if (!active) return;
      if (profileResult.status === "fulfilled") {
        setProfile((current) => {
          const nextProfile = mergeProfile(current, profileResult.value);
          applyDocumentProfile(nextProfile);
          return nextProfile;
        });
      }
      if (menusResult.status === "fulfilled") setMenus(menusResult.value);
    });
    return () => { active = false; };
  }, []);

  return { profile, menus };
}
