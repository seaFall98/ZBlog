export default defineEventHandler(async event => {
  const config = useRuntimeConfig();
  const apiBase = (config.apiServerUrl || config.public.apiUrl) as string;
  const backendUrl = apiBase.replace(/\/+$/, '').replace(/\/api\/v\d+$/, '');

  try {
    const response = await fetch(`${backendUrl}/rss.xml`);

    if (!response.ok) {
      throw new Error(`Backend returned ${response.status}`);
    }

    const rssFeed = await response.text();
    setResponseHeader(event, 'Content-Type', 'application/rss+xml; charset=utf-8');
    return rssFeed;
  } catch {
    throw createError({
      statusCode: 500,
      statusMessage: 'Failed to fetch RSS feed',
    });
  }
});
