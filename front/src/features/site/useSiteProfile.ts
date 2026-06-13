import { createContext, createElement, useContext, useEffect, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
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
  guestbookDanmakuLimit: 200,
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
  const {
    data: profileData,
    isLoading: profileLoading,
    error: profileError,
  } = useQuery({
    queryKey: ["siteProfile"],
    queryFn: fetchSiteProfile,
  });
  const {
    data: menusData,
    isLoading: menusLoading,
    error: menusError,
  } = useQuery({
    queryKey: ["siteMenus"],
    queryFn: fetchMenuGroups,
  });

  useEffect(() => {
    applyDocumentProfile(profileData ?? emptySiteProfile);
  }, [profileData]);

  const value = useMemo<SiteProfileContextValue>(() => {
    const loading = profileLoading || menusLoading;
    return {
      profile: profileData ?? emptySiteProfile,
      headerMenus: menusData?.header ?? [],
      footerMenus: menusData?.footer ?? [],
      loading,
      loaded: !loading,
      error: profileError ?? menusError ?? null,
    };
  }, [menusData, menusError, menusLoading, profileData, profileError, profileLoading]);

  return createElement(SiteProfileContext.Provider, { value }, children);
}

export function useSiteProfile() {
  return useContext(SiteProfileContext);
}
