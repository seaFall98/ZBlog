#!/bin/sh

# 默认 API URL
DEFAULT_API_URL="http://localhost:8080/api/v1"

# 生成运行时配置文件
cat > /usr/share/nginx/html/config.js << EOF
window.__APP_CONFIG__ = {
  apiUrl: "${API_URL:-$DEFAULT_API_URL}"
};
EOF

echo "API URL configured: ${API_URL:-$DEFAULT_API_URL}"

# 启动 nginx
exec nginx -g 'daemon off;'
