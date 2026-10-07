# syntax=docker/dockerfile:1
#
# Imagem do frontend App. O build de produção troca environment.ts por environment.prod.ts
# (fileReplacements em angular.json), mas os dois só definem DEFAULTS compilados: o valor real de
# runtime vem de env.js, gerado pelo docker-entrypoint.sh a partir de variáveis de ambiente do
# contêiner — é o que permite repontar a API sem rebuild.

# ---------------------------------------------------------------------------
# Stage 1 — build
# ---------------------------------------------------------------------------
FROM node:22 AS build

# Credenciais do registry privado GEMS (@gabriel-mdias/angular-gems-sdk).
#
# O pacote NÃO resolve sem credencial: npm.pkg.github.com responde 401 com token ausente.
# Na máquina do desenvolvedor ele resolve porque o ~/.npmrc global tem _authToken; num build limpo,
# não. O CI monta GEMS_REGISTRY_TOKEN como secret BuildKit.
#
# O `.npmrc` é criado e removido na mesma camada que instala as dependências.

WORKDIR /workspace

COPY package.json package-lock.json ./

# A SDK 2.3.0 e keycloak-angular 22 resolvem os peers no Angular 22.
RUN --mount=type=secret,id=gems_registry_token,required=true \
    printf '@gabriel-mdias:registry=https://npm.pkg.github.com\n//npm.pkg.github.com/:_authToken=%s\n' "$(cat /run/secrets/gems_registry_token)" > .npmrc \
    && npm ci \
    && rm .npmrc

COPY . .

RUN npx ng build --configuration production

# ---------------------------------------------------------------------------
# Stage 2 — runtime
# ---------------------------------------------------------------------------
FROM nginx:alpine

# O nome do projeto Angular é "app" (frontend/angular.json) — o builder @angular/build:application
# escreve o output do browser em dist/<projeto>/browser, não em dist/ direto.
COPY --from=build /workspace/dist/app/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY docker-entrypoint.sh /docker-entrypoint-app.sh

RUN chmod +x /docker-entrypoint-app.sh

EXPOSE 80

# O entrypoint gera env.js a partir das variáveis de ambiente do contêiner e só então inicia o
# nginx em foreground (o próprio script termina com `exec nginx -g 'daemon off;'`).
ENTRYPOINT ["/docker-entrypoint-app.sh"]
