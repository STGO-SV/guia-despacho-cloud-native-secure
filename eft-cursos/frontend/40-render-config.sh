#!/bin/sh
set -eu

for name in B2C_CLIENT_ID B2C_AUTHORITY B2C_KNOWN_AUTHORITY B2C_REDIRECT_URI B2C_SCOPE; do
  if [ -z "$(printenv "$name" 2>/dev/null || true)" ]; then
    echo "Configuracion publica requerida no definida: $name" >&2
    exit 1
  fi
done

export BFF_BASE_URL="${BFF_BASE_URL:-}"
envsubst '${B2C_CLIENT_ID} ${B2C_AUTHORITY} ${B2C_KNOWN_AUTHORITY} ${B2C_REDIRECT_URI} ${B2C_SCOPE} ${BFF_BASE_URL}' \
  < /opt/eft/config.template.js \
  > /usr/share/nginx/html/config.js
