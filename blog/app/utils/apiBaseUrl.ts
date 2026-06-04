export function resolveApiBaseUrl() {
  const config = useRuntimeConfig();
  return import.meta.server
    ? ((config.apiServerUrl || config.public.apiUrl) as string)
    : (config.public.apiUrl as string);
}
