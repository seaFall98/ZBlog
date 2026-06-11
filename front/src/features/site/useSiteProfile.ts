import { createContext, createElement, useContext, useEffect, useMemo, useState } from "react";
import { fetchMenuGroups, fetchSiteProfile } from "./siteApi";
import type { SiteMenuView, SiteProfileView } from "./types";

export const emptySiteProfile: SiteProfileView = {
  title: "",
  ownerDisplayName: "",
  subtitle: "",
  aboutIntro: "",
  email: "",
  avatarUrl: "",
  faviconUrl: "",
  established: "",
  icpRecord: "",
  policeRecord: "",
  heroEyebrow: "",
  heroTitle: "",
  heroMeta: "",
  heroCtaLabel: "",
  heroCtaTarget: "",
  heroSlogan: "",
  footerDescription: "",
  footerCopyright: "",
  footerSlogan: "",
  socialLinks: [],
  backgroundImage: "",
  barrageBackgroundImage: "",
  guestbookIntro: "",
  aboutStatusItems: [],
  aboutSkillItems: [],
  aboutTimelineItems: [],
  aboutBottomQuote: "",
};

type SiteProfileContextValue = {
  profile: SiteProfileView;
  headerMenus: SiteMenuView[];
  footerMenus: SiteMenuView[];
  loading: boolean;
  loaded: boolean;
  error: unknown;
};

const initialValue: SiteProfileContextValue = {
  profile: emptySiteProfile,
  headerMenus: [],
  footerMenus: [],
  loading: true,
  loaded: false,
  error: null,
};

const SiteProfileContext = createContext<SiteProfileContextValue>(initialValue);

function applyDocumentProfile(profile: SiteProfileView) {
  if (typeof document === "undefined") return;

  if (profile.title) {
    document.title = profile.title;
  }

  if (!profile.faviconUrl) return;

  let favicon = document.querySelector<HTMLLinkElement>('link[rel~="icon"]');
  if (!favicon) {
    favicon = document.createElement("link");
    favicon.rel = "icon";
    document.head.appendChild(favicon);
  }
  favicon.href = profile.faviconUrl;
}

export function SiteProfileProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<SiteProfileContextValue>(initialValue);

  useEffect(() => {
    let active = true;

    async function load() {
      const [profileResult, menusResult] = await Promise.allSettled([fetchSiteProfile(), fetchMenuGroups()]);
      if (!active) return;

      const profile = profileResult.status === "fulfilled" ? profileResult.value : emptySiteProfile;
      const headerMenus = menusResult.status === "fulfilled" ? menusResult.value.header : [];
      const footerMenus = menusResult.status === "fulfilled" ? menusResult.value.footer : [];
      const error =
        profileResult.status === "rejected"
          ? profileResult.reason
          : menusResult.status === "rejected"
            ? menusResult.reason
            : null;

      applyDocumentProfile(profile);
      setState({
        profile,
        headerMenus,
        footerMenus,
        loading: false,
        loaded: true,
        error,
      });
    }

    void load();

    return () => {
      active = false;
    };
  }, []);

  const value = useMemo(() => state, [state]);
  return createElement(SiteProfileContext.Provider, { value }, children);
}

export function useSiteProfile() {
  return useContext(SiteProfileContext);
}
