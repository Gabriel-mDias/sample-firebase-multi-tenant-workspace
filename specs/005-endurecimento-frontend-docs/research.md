# Research — 005 Endurecimento do frontend, documentação e scripts

Apurado em 2026-10-12 contra o código da branch (base `origin/main` `5b849a3`). O código vence.

## R1 — Política de conteúdo (CSP)

- **Fatos:**
  - o `index.html` do build não tem script nem estilo inline: só `env.js`, o CSS e os módulos com hash;
  - o login usa `signInWithPopup` com `GoogleAuthProvider` (`firebase-web.adapter.ts`). O SDK Web do
    Firebase carrega `https://apis.google.com/js/api.js` e abre um iframe em
    `https://<authDomain>/__/auth/iframe`;
  - as chamadas de identidade vão para `identitytoolkit.googleapis.com` e `securetoken.googleapis.com`;
  - o Angular injeta os estilos de componente em `<style>` em tempo de execução;
  - o `authDomain` muda por ambiente (`FIREBASE_AUTH_DOMAIN`) e a API pode ser absoluta
    (`API_BASE_URL`).
- **Decisão:** a política é gerada pelo `docker-entrypoint.sh`, no mesmo bloco Python que já valida a
  configuração pública, e gravada num arquivo incluído pelo nginx. Diretivas:
  - `default-src 'self'`, `object-src 'none'`, `base-uri 'self'`, `form-action 'self'`, `frame-ancestors 'none'`;
  - `script-src 'self' https://apis.google.com`;
  - `style-src 'self' 'unsafe-inline'`, exigido pelos estilos de componente do Angular; `img-src 'self' data:`;
    `font-src 'self'`;
  - `connect-src 'self' https://identitytoolkit.googleapis.com https://securetoken.googleapis.com`, mais a
    origem da API quando ela é absoluta e o emulador só em perfil local;
  - `frame-src https://<authDomain>`.
- **Alternativa descartada:** CSP fixa no `nginx.conf`. Ela não acompanha o `authDomain` por ambiente,
  e teria de liberar `*.firebaseapp.com` inteiro.
- **Alternativa descartada:** nonce por requisição. Não há script inline a proteger, e o nginx estático
  não gera nonce sem módulo extra.
- **Prova:** login com Google e por senha em tela, sem violação no console (C3). Se aparecer violação de
  algum domínio não listado, ele entra só com a evidência da violação.

## R2 — Demais headers e a herança do `add_header`

- **Fato:** no nginx, um `add_header` dentro de uma `location` descarta os herdados do `server`. A
  `location = /env.js` já declara `Cache-Control`.
- **Decisão:** os headers ficam num arquivo gerado, `/etc/nginx/gems/security-headers.conf`, incluído
  no `server` e em toda `location` que declara `add_header` própria. São eles: CSP,
  `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`,
  `Referrer-Policy: strict-origin-when-cross-origin`,
  `Permissions-Policy: camera=(), microphone=(), geolocation=()`,
  `Cross-Origin-Opener-Policy: same-origin-allow-popups` e `X-Permitted-Cross-Domain-Policies: none`.
  Todos levam `always`, para valerem também em respostas de erro.
- O diretório `gems/` fica fora de `conf.d/`, para o nginx não incluir o arquivo no nível `http`.

## R3 — Cache

- **Fato:** o build gera `main-<HASH>.js`, `chunk-<hash>.js` e `styles-<HASH>.css`, com 8 caracteres
  alfanuméricos.
- **Decisão:** a `location ~* -[A-Za-z0-9]{8}\.(?:js|css)$` recebe
  `Cache-Control: public, max-age=31536000, immutable`. O `index.html` (em `/` e no fallback do SPA)
  recebe `Cache-Control: no-cache`, e o `/env.js` mantém `no-store`. O restante (ícone, marca) fica no
  padrão do nginx.

## R4 — `robots.txt`

- **Fato:** `frontend/public/robots.txt` não existe, e o `try_files ... /index.html` devolve o
  `index.html` no lugar dele.
- **Decisão:** criar `frontend/public/robots.txt` com `User-agent: *`, `Allow: /login` e
  `Disallow: /`. A área autenticada não é indexada, e a `/login` continua rastreável, que é o que o
  Lighthouse avalia. A `location = /robots.txt` usa `try_files $uri =404`.

## R5 — Upstream da API

- **Fato:** `proxy_pass http://backend:8080` resolve o host na subida. Fora do Compose, o nginx não
  sobe sem `--add-host`.
- **Decisão:**
  - variável `API_UPSTREAM`, com padrão `http://backend:8080`, validada pelo entrypoint: esquema http ou
    https, hostname válido, porta opcional, sem caminho, query, fragmento ou credencial;
  - o entrypoint grava `/etc/nginx/gems/api-upstream.conf`, com `resolver <nameserver do
    /etc/resolv.conf> valid=30s`, `set $api_upstream <valor>` e `proxy_pass $api_upstream`. Com variável,
    o nginx resolve o nome na requisição, não na subida;
  - sem `URI` no `proxy_pass`, o caminho original segue igual ao de hoje.
- **Recusa:** valor inválido ou `resolv.conf` sem `nameserver` encerram a subida com mensagem fixa, sem
  ecoar o valor.
- **Compose:** não muda. O padrão é o mesmo endereço de hoje.

## R6 — Testes

- **Fato:** `scripts/test_runtime_config.py` já roda o Python do entrypoint num diretório temporário e
  cobre a validação da configuração pública (8 testes).
- **Decisão:** estender esse arquivo com:
  - CSP com o `authDomain` em `frame-src`;
  - origem absoluta da API em `connect-src`, e nenhuma origem extra com API relativa;
  - emulador só em perfil local;
  - upstream padrão, upstream válido e recusa de upstream inválido, sem eco do valor;
  - `resolv.conf` sem `nameserver`.

  O caminho de saída dos arquivos do nginx fica injetável por variável, como já é o `ENV_JS_PATH`.
- **Smoke do container (C3):** `docker build` e `docker run` sem host `backend`, depois `curl -I` em `/`,
  num asset com hash, em `/env.js`, em `/robots.txt` e em `/healthz`, além de `nginx -t`.

## R7 — Documentação

- **README.md:** as linhas 10–11, 89, 98 e 101 ainda descrevem como pendentes o E2E integral, o
  cleanup/rename, o benchmark e a homologação. Todos já foram provados: 003 (T029, T028, T030 e
  homologação local) e 006 (A10 e A11).
- **Guia:** `specs/003-firebase-multi-tenant/configuracao-homologacao-firebase.md` §5 lista as variáveis
  do backend e não cita `APP_SECURITY_CORS_ALLOWED_ORIGINS`. A propriedade é
  `app.security.cors.allowed-origins`, com padrão `http://localhost:4200` (`SecurityConfig.java`). Ela
  entra no bloco de variáveis com o exemplo do frontend local na porta 4280.

## R8 — `.pyc` e o manifesto do clean-demo

- **Fato:** `scripts/__pycache__/test_runtime_config.cpython-314.pyc` e
  `scripts/__pycache__/verify-operational-legacy.cpython-314.pyc` estão versionados, e o `.gitignore`
  não ignora `__pycache__/`. O `.gitignore` é metadado protegido do `scripts/demo-manifest.json` (006,
  R11).
- **Decisão:** `git rm --cached` nos dois `.pyc`, `__pycache__/` no `.gitignore`, e o hash desse metadado
  e o pin `MANIFEST_SHA256` atualizados no mesmo commit. O `sample.py` já ignora `__pycache__`
  (`PRUNED`).
- **Prova:** `unittest` verde (60) e `clean-demo --dry-run` aceito. O `docker-entrypoint.sh` também é
  metadado do inicializador (`INITIALIZER_METADATA`), então a A11 da 006 roda de novo sobre a 005.

## R9 — Dependência de ordem

A 005 começa depois do merge da 006 no sample. Ela usa o `sample.py` e o manifesto regenerados lá, e a
mudança no `.gitignore` e no entrypoint precisa da prova do inicializador já no formato novo.
