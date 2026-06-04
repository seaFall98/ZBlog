#!/bin/sh

# 默认 API URL
DEFAULT_API_URL="http://localhost:8080/api/v1"
API_BASE_URL="${API_URL:-$DEFAULT_API_URL}"
BACKEND_ORIGIN="${API_BASE_URL%/api/v1}"
FAVICON_URL=""

if BLOG_SETTINGS=$(curl -fsS "${API_BASE_URL}/settings/blog" 2>/dev/null); then
  BLOG_FAVICON=$(printf '%s' "$BLOG_SETTINGS" | sed -n 's/.*"blog.favicon":"\([^"]*\)".*/\1/p')
  case "$BLOG_FAVICON" in
    /uploads/*)
      FAVICON_URL="${BACKEND_ORIGIN}${BLOG_FAVICON}"
      ;;
    http://*|https://*)
      FAVICON_URL="$BLOG_FAVICON"
      ;;
  esac
fi

# 生成运行时配置文件
cat > /usr/share/nginx/html/config.js << EOF
window.__APP_CONFIG__ = {
  apiUrl: "${API_BASE_URL}",
  faviconUrl: "${FAVICON_URL}"
};
EOF

echo "API URL configured: ${API_BASE_URL}"
echo "Favicon URL configured: ${FAVICON_URL:-fallback to /favicon.ico}"

# 启动 nginx
exec nginx -g 'daemon off;'
