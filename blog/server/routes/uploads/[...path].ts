export default defineEventHandler(async event => {
  const path = event.context.params?.path;
  const assetPath = Array.isArray(path) ? path.join('/') : path;
  if (!assetPath) {
    throw createError({ statusCode: 404, statusMessage: 'Not Found' });
  }

  const config = useRuntimeConfig();
  const apiUrl = (config.apiServerUrl || config.public.apiUrl || '').replace(/\/api\/v\d+\/?$/, '');
  if (!apiUrl) {
    throw createError({ statusCode: 503, statusMessage: 'Asset backend is not configured' });
  }

  return proxyRequest(event, `${apiUrl}/uploads/${assetPath}`);
});
