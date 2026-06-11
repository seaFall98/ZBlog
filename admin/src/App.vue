<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { getPublicSettingGroup } from '@/api/sysconfig';

const defaultApiUrl = 'http://localhost:8080/api/v1';

const getApiUrl = () => window.__APP_CONFIG__?.apiUrl || import.meta.env.VITE_API_URL || defaultApiUrl;

const getBackendOrigin = () => getApiUrl().replace(/\/api\/v\d+\/?$/, '').replace(/\/$/, '');

const resolveBackendUrl = (href: string) => {
  const favicon = href.trim();
  if (!favicon) return '';
  if (favicon.startsWith('http://') || favicon.startsWith('https://')) return favicon;
  if (favicon.startsWith('/uploads/')) return `${getBackendOrigin()}${favicon}`;
  return favicon;
};

const applyFavicon = (href: string) => {
  const favicon = resolveBackendUrl(href);
  if (!favicon) return;

  ['icon', 'shortcut icon'].forEach(rel => {
    let link = document.querySelector<HTMLLinkElement>(`link[rel="${rel}"]`);
    if (!link) {
      link = document.createElement('link');
      link.rel = rel;
      document.head.appendChild(link);
    }
    link.href = favicon;
  });
};

applyFavicon(window.__APP_CONFIG__?.faviconUrl || '/favicon.ico');

onMounted(async () => {
  try {
    const blogConfig = await getPublicSettingGroup('blog');
    const favicon = blogConfig['blog.favicon'] || blogConfig.favicon || window.__APP_CONFIG__?.faviconUrl || '/favicon.ico';
    applyFavicon(favicon);
  } catch {
    applyFavicon(window.__APP_CONFIG__?.faviconUrl || '/favicon.ico');
  }
});
</script>

<style>
#app {
  width: 100%;
  height: 100vh;
}
</style>
