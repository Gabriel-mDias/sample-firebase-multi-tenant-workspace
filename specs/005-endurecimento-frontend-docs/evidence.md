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

## F12 — A11 da 006 sobre a 005

Variante custom (`com.acme`) num clone descartável de `875713d`, em `.tooling/a11-005`: `initialize` com dry-run, apply, replay do formatter e reapply recusado; `clean-demo` com dry-run e apply; `check-java --all`; `mvnw clean install` (1 + 69 + 189 testes, 1 ignorado pelo `T029_FIREBASE_REAL=skip` desta cópia, 0 Spotless); e frontend com test, build e prettier. Resultado: `RESULT PASS`. O backend com Firebase real da 005 fica provado pela CI do PR, porque a 005 não toca no backend.

## F9 — Lighthouse da `/login` depois (2026-10-12)

Lighthouse 12.6.1, Chrome headless, sobre a imagem `gems-005-frontend:smoke` em `http://localhost:4280`. A configuração pública do Firebase foi copiada do container da homologação pelo Docker, sem exibir os valores.

| Categoria | Antes (003) | Depois |
| :--- | :-: | :-: |
| Performance | 52 | 61 |
| Acessibilidade | 100 | 100 |
| Melhores práticas | 77 | 96 |
| SEO | 83 | 92 |

`errors-in-console` veio vazio, então o carregamento da `/login` não gerou nenhuma violação de CSP. `csp-xss` passou. Ficaram dois itens: `font-size` em Melhores práticas e `meta-description` em SEO, ambos fora do escopo da 005.

**F8, login em tela com a CSP:** pendente com o product owner. Exige o backend com Firebase real e o login por Google e por senha (quickstart F8).

## T016 — Review independente

O Sol (`gpt-6.1-sol`, read-only, esforço medium) revisou `f768fbe..HEAD` e deu **PASS**, sem finding high. Os critérios 1 a 9 passaram: CSP mínima, valores validados, herança de `add_header`, cache, upstream, escrita atômica em `sh`, Compose intacto, documentação sem segredo e testes significativos.

Houve dois findings medium:
- **Escopo do diff:** ele inclui o fechamento da 006 e a troca do `.specify/feature.json`. Foi intencional. O commit `541579b` fecha a T029 da 006 depois do merge, e a única branch pendente em que ele cabia era esta. O PR vai explicar.
- **F8, F9 e F12 sem registro na hora do review:** F12 e F9 foram registradas acima. F8 fica com o product owner.
