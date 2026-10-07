#!/bin/sh
# Gera /usr/share/nginx/html/env.js a partir de variáveis de ambiente do contêiner, no formato
# que frontend/src/environments/environment.ts já espera via buildEnvironment() (RuntimeEnvironmentOverrides
# em environment.model.ts) — é o que permite repontar a SPA para outra API sem reconstruir a imagem
# (FR-014/SC-007).
#
# Cada variável ausente OMITE a chave correspondente em vez de escrever string vazia: o
# buildEnvironment() faz merge raso sobre os defaults compilados (environment.prod.ts), então uma
# chave ausente preserva o default; uma chave com "" sobrescreveria o default com um valor vazio.
set -eu

ENV_JS_PATH="/usr/share/nginx/html/env.js"

# Monta o objeto oidc só com as chaves que vieram definidas, para não gerar `"issuer": undefined`
# nem forçar o merge a apagar o default com string vazia.
oidc_fields=""
if [ -n "${OIDC_ISSUER:-}" ]; then
  oidc_fields="${oidc_fields}\"issuer\":\"${OIDC_ISSUER}\","
fi
if [ -n "${OIDC_CLIENT_ID:-}" ]; then
  oidc_fields="${oidc_fields}\"clientId\":\"${OIDC_CLIENT_ID}\","
fi
oidc_fields="${oidc_fields%,}"

fields=""
if [ -n "${API_BASE_URL:-}" ]; then
  fields="${fields}\"apiBaseUrl\":\"${API_BASE_URL}\","
fi
if [ -n "$oidc_fields" ]; then
  fields="${fields}\"oidc\":{${oidc_fields}},"
fi
fields="${fields%,}"

echo "globalThis.__env = {${fields}};" > "$ENV_JS_PATH"

exec nginx -g 'daemon off;'
