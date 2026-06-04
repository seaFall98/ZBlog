/**
 * 图片搜索API代理服务
 * 支持 Unsplash、Pixabay 和 Pexels 平台
 */

// CORS配置
const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
}

// 缓存配置
const CACHE_CONFIG = {
  TTL: 300, // 缓存时间（秒）
  PERIOD: 300000 // 缓存周期（毫秒）
}

// 平台配置
const PLATFORMS = {
  unsplash: {
    baseUrl: 'https://api.unsplash.com',
    version: 'v1',
    authHeader: 'Client-ID',
    authPrefix: '',
    endpoints: {
      search: '/search/photos',
      default: '/photos'
    }
  },
  pixabay: {
    baseUrl: 'https://pixabay.com',
    version: '',
    authHeader: 'key',
    authPrefix: '',
    endpoints: {
      search: '/api/',
      default: '/api/'
    }
  },
  pexels: {
    baseUrl: 'https://api.pexels.com/v1',
    version: '',
    authHeader: 'Authorization',
    authPrefix: 'Bearer ',
    endpoints: {
      search: '/search',
      default: '/curated'
    }
  }
}

// 缓存管理
class CacheManager {
  constructor() {
    this.cache = caches.default
  }

  async get(request) {
    try {
      const cached = await this.cache.match(request)
      if (cached) {
        const response = new Response(cached.body, cached)
        response.headers.set('X-Cache', 'HIT')
        return response
      }
    } catch (e) {
      console.error('缓存读取失败:', e)
    }
    return null
  }

  async put(request, response) {
    if (request.method !== 'GET' || !response.ok) return

    const [body1, body2] = response.body.tee()
    const cacheResponse = new Response(body1, response)
    cacheResponse.headers.set('X-Cache', 'MISS')
    cacheResponse.headers.set('Cache-Control', `public, max-age=${CACHE_CONFIG.TTL}`)

    const userResponse = new Response(body2, response)

    this.cache.put(request, cacheResponse).catch(e => {
      console.error('缓存写入失败:', e)
    })

    return userResponse
  }
}

// 处理OPTIONS请求
function handleOptions() {
  return new Response(null, {
    status: 200,
    headers: corsHeaders
  })
}

// 统一响应格式
function unifyResponse(platform, data, page = 1, page_size = 20) {
  if (platform === 'unsplash') {
    let photos
    let total = -1

    if (Array.isArray(data)) {
      photos = data
    } else {
      photos = data.results || []
      total = data.total || -1
    }

    return {
      platform: 'unsplash',
      page: page,
      page_size: page_size,
      total: total,
      results: photos.map(item => ({
        id: item.id,
        title: item.alt_description || item.description || '',
        url: item.urls?.regular,
        thumbnail: item.urls?.thumb,
        download: item.urls?.full,
        width: item.width,
        height: item.height,
        photographer: item.user?.name,
        photographer_url: item.user?.links?.html,
        unsplash_url: item.links?.html,
        tags: item.tags?.map(tag => tag.title) || []
      }))
    }
  } else if (platform === 'pixabay') {
    const photos = data.hits || []
    const total = data.totalHits || data.total || -1

    return {
      platform: 'pixabay',
      page: page,
      page_size: page_size,
      total: total,
      results: photos.map(item => ({
        id: item.id,
        title: item.tags || '',
        url: item.webformatURL,
        thumbnail: item.previewURL,
        download: item.largeImageURL,
        width: item.imageWidth,
        height: item.imageHeight,
        photographer: item.user,
        photographer_url: `https://pixabay.com/users/${item.user}-${item.user_id}/`,
        pixabay_url: item.pageURL,
        tags: item.tags ? item.tags.split(', ') : []
      }))
    }
  } else if (platform === 'pexels') {
    const photos = data.photos || []
    const total = data.total_results || -1

    return {
      platform: 'pexels',
      page: page,
      page_size: page_size,
      total: total,
      results: photos.map(item => ({
        id: item.id,
        title: item.alt || '',
        url: item.src?.large,
        thumbnail: item.src?.medium,
        download: item.src?.original,
        width: item.width,
        height: item.height,
        photographer: item.photographer,
        photographer_url: item.photographer_url,
        pexels_url: item.url,
        tags: []
      }))
    }
  }

  return data
}

// 处理统一搜索
async function handleUnifiedSearch(request, env) {
  const url = new URL(request.url)
  const platform = url.searchParams.get('platform') || 'unsplash'

  if (!PLATFORMS[platform]) {
    return new Response(JSON.stringify({ error: '不支持的平台' }), {
      status: 400,
      headers: { 'Content-Type': 'application/json', ...corsHeaders }
    })
  }

  const query = url.searchParams.get('query')
  const page = url.searchParams.get('page') || '1'
  const page_size = url.searchParams.get('page_size') || url.searchParams.get('per_page') || '20'

  const endpointType = query ? 'search' : 'default'
  const targetPath = PLATFORMS[platform].endpoints[endpointType]
  const targetUrl = new URL(PLATFORMS[platform].baseUrl + targetPath)

  if (query) {
    targetUrl.searchParams.set('query', query)
  }
  targetUrl.searchParams.set('page', page)
  targetUrl.searchParams.set('per_page', page_size)

  if (platform === 'unsplash') {
    targetUrl.searchParams.set('client_id', env.UNSPLASH_ACCESS_KEY)
  } else if (platform === 'pixabay') {
    targetUrl.searchParams.set('key', env.PIXABAY_API_KEY)
  } else if (platform === 'pexels') {
    // Pexels使用Authorization header
  }

  const cacheManager = new CacheManager()
  const cacheUrl = new URL(targetUrl.toString())
  const now = Math.floor(Date.now() / CACHE_CONFIG.PERIOD)
  cacheUrl.searchParams.set('_cache', String(now))
  const cacheKey = new Request(cacheUrl.toString())
  const cached = await cacheManager.get(cacheKey)
  if (cached) {
    return cached
  }

  try {
    const headers = {
      'Accept': 'application/json',
      'Accept-Version': PLATFORMS[platform].version || undefined,
      'User-Agent': 'ImageProxy/1.0'
    }

    if (platform === 'pexels') {
      headers['Authorization'] = `${PLATFORMS[platform].authPrefix}${env.PEXELS_API_KEY}`
    }

    const response = await fetch(targetUrl.toString(), {
      method: request.method,
      headers
    })

    if (!response.ok) {
      const errorText = await response.text()
      console.error(`API请求失败 [${platform}]:`, response.status, errorText)

      return new Response(JSON.stringify({
        error: 'API请求失败',
        platform,
        status: response.status,
        message: errorText
      }), {
        status: response.status,
        headers: { 'Content-Type': 'application/json', ...corsHeaders }
      })
    }

    const data = await response.json()
    const unifiedData = unifyResponse(platform, data, parseInt(page), parseInt(page_size))

    const finalResponse = new Response(JSON.stringify(unifiedData), {
      status: 200,
      headers: {
        'Content-Type': 'application/json',
        ...corsHeaders
      }
    })

    return cacheManager.put(cacheKey, finalResponse)

  } catch (error) {
    console.error('请求处理失败:', error)
    return new Response(JSON.stringify({
      error: '服务内部错误',
      message: error.message
    }), {
      status: 500,
      headers: { 'Content-Type': 'application/json', ...corsHeaders }
    })
  }
}

// 主处理函数
export default {
  async fetch(request, env) {
    const url = new URL(request.url)

    if (request.method === 'OPTIONS') {
      return handleOptions()
    }

    if (url.pathname === '/') {
      return handleUnifiedSearch(request, env)
    }

    if (url.pathname === '/health') {
      return new Response(JSON.stringify({
        status: 'ok',
        platforms: Object.keys(PLATFORMS),
        timestamp: new Date().toISOString()
      }), {
        headers: { 'Content-Type': 'application/json' }
      })
    }

    return new Response(JSON.stringify({
      error: 'Not Found',
      available_endpoints: [
        'GET /?platform=unsplash|pixabay|pexels&query=xxx',
        'GET /health'
      ]
    }), {
      status: 404,
      headers: { 'Content-Type': 'application/json', ...corsHeaders }
    })
  }
}
