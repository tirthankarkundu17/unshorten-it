#!/bin/sh
# Write environment variables to env-config.js

# Alpine uses ash. The most robust way to strip quotes is using tr
CLEAN_URL=${API_BASE_URL:-"http://127.0.0.1:8000"}
CLEAN_URL=$(echo "$CLEAN_URL" | tr -d '"' | tr -d "'")

cat <<EOF > /usr/share/nginx/html/env-config.js
window._env_ = {
  API_BASE_URL: "${CLEAN_URL}"
};
EOF
