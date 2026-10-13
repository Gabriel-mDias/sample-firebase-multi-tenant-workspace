# Evidências — 005 Endurecimento do frontend, documentação e scripts

## T001 — Linha de base (2026-10-12)

A branch foi rebaseada sobre a `origin/main` `f768fbe` (merge da 006, PR #12).

**Headers.** Fonte: `curl -I` no container `gems-local-real-frontend:local`, imagem de 2026-10-12 19:28 UTC.
Ela foi construída para a homologação local da 003, e a 006 não tocou no frontend. A porta é `127.0.0.1:4280`.

| Caminho | Status | `Content-Type` | `Cache-Control` | Headers de segurança |
| :--- | :--- | :--- | :--- | :--- |
| `/` | 200 | `text/html` | nenhum | nenhum |
| `/login` | 200 | `text/html` | nenhum | nenhum |
| `/env.js` | 200 | `application/javascript` | **dois**: `no-cache` e `no-store, no-cache, must-revalidate` | nenhum |
| `/main-QXMPOT35.js` (asset com hash) | 200 | — | nenhum | nenhum |
| `/robots.txt` | 200 | `text/html` | nenhum | nenhum. O corpo é o `index.html` |

**Lighthouse da `/login`.** A linha de base é a medição da homologação da 003: Performance 52,
Acessibilidade 100, Melhores práticas 77 e SEO 83. O frontend não mudou desde então.

## T002–T008 — Testes primeiro e implementação

- O pacote 005-A, escrito pelo Sol, acrescentou 10 testes a `scripts/test_runtime_config.py`. O
  coordenador os rodou contra o entrypoint antigo: 20 testes, **os 10 novos vermelhos** pelos motivos
  esperados (`security-headers.conf` e `api-upstream.conf` ausentes, upstream inválido aceito,
  `resolv.conf` ignorado) e os 10 existentes verdes.
- O pacote 005-B, também do Sol, fez a implementação. Ele ajustou só a fixture dos testes antigos, que
  passou a injetar os caminhos temporários novos; nenhuma asserção mudou. Resultado: 20 de 20 verdes.
- Testes Python inteiros: `python -B -m unittest discover -s scripts -p "test_*.py"`, 70 verdes.
- T014: `.pyc` fora do git, `__pycache__/` no `.gitignore`, manifesto regenerado (só a entrada do
  `.gitignore` mudou) e pin `MANIFEST_SHA256` atualizado. `clean-demo --dry-run` foi aceito num clone
  limpo (exit 0); a worktree é recusada por não ser um checkout autônomo, por projeto.

## T009 — Smoke do container (2026-10-12)

Imagem `gems-005-frontend:smoke`, gerada por `npm ci`, `npm run build` e `docker build`. O container
subiu com valores fictícios de Firebase, perfil `production` e **sem host `backend`**.

- **F2:** `nginx -t` → `syntax is ok` / `test is successful`.
- **F3:** o container ficou `healthy`, e o `/healthz` respondeu 200.
- **F4 e F5:**

  | Caminho | Status | `Content-Type` | `Cache-Control` | Headers de segurança |
  | :--- | :--- | :--- | :--- | :--- |
  | `/` | 200 | `text/html` | `no-cache` | 7 |
  | `/login` | 200 | `text/html` | `no-cache` | 7 |
  | `/env.js` | 200 | `application/javascript` | `no-store` (um só) | 7 |
  | `/main-QXMPOT35.js` | 200 | `application/javascript` | `public, max-age=31536000, immutable` | 7 |
  | `/robots.txt` | 200 | `text/plain` | — | 7 |
  | `/healthz` | 200 | `text/plain` | — | 7 |
  | `/nao-existe.png` (fallback SPA) | 200 | `text/html` | `no-cache` | 7 |

  A CSP observada foi `default-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self';
  frame-ancestors 'none'; script-src 'self' https://apis.google.com; style-src 'self' 'unsafe-inline';
  img-src 'self' data:; font-src 'self'; connect-src 'self' https://identitytoolkit.googleapis.com
  https://securetoken.googleapis.com; frame-src https://<authDomain>`.
- **F6:** `/robots.txt` vem como `text/plain`, com `User-agent: *`, `Allow: /login` e `Disallow: /`.
- **F7:**
  - Com `API_UPSTREAM` contendo usuário e senha, a subida é recusada (exit 1) com a mensagem `Invalid
    API_UPSTREAM; expected an HTTP or HTTPS origin`, sem ecoar o valor.
  - Com `API_UPSTREAM=http://gems005api:80`, um nginx numa rede Docker própria, `GET /api/ping` chegou ao
    upstream como `GET /api/ping`, com o caminho preservado, e voltou 404 do upstream.

Os containers e a rede do smoke foram removidos.
## F10 e F13 local

- `npm test -- --watch=false`: 419 verdes; `npm run build` verde; `npx prettier --check .` verde.
- `scripts/gems-ai-gate.ps1` (CLI 3.1.0 da fonte `.tooling/gems-ai-sdk/src` no pin `909c69a`): exit 0. `verificar-contexto-agentes.ps1`: aprovado.
