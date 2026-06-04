export function resolvePublicSiteUrl() {
  const config = useRuntimeConfig();
  const configured = ((config.public.siteUrl || '') as string).trim().replace(/\/+$/, '');
  if (configured) {
    return configured;
  }

  if (import.meta.server) {
    const requestUrl = useRequestURL();
    return `${requestUrl.protocol}//${requestUrl.host}`.replace(/\/+$/, '');
  }

  if (import.meta.client) {
    return window.location.origin.replace(/\/+$/, '');
  }

  return '';
}

export function joinPublicUrl(pathOrUrl?: string | null, baseUrl = '') {
  if (!pathOrUrl || !pathOrUrl.trim()) {
    return baseUrl || '/';
  }

  const value = pathOrUrl.trim();
  if (/^https?:\/\//i.test(value)) {
    return value;
  }

  if (!baseUrl) {
    return value;
  }

  return `${baseUrl.replace(/\/+$/, '')}${value.startsWith('/') ? value : `/${value}`}`;
}

export function resolvePublicUrl(pathOrUrl?: string | null) {
  return joinPublicUrl(pathOrUrl, resolvePublicSiteUrl());
}
