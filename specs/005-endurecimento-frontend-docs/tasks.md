# Tasks: Endurecimento do frontend, documentação e scripts

**Input**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [quickstart.md](quickstart.md)
**Tests**: o comportamento novo do entrypoint vem com teste primeiro (`scripts/test_runtime_config.py`).
A borda do nginx é provada por smoke do container.

Formato: `- [ ] Tnnn [P] [USn] descrição com caminho`.

## Phase 1: Setup

- [ ] T001 Depois do merge da 006 no sample, rebasear `feature/005-endurecimento-frontend-docs` sobre a `origin/main` e medir a linha de base: Lighthouse na `/login` do container atual (Performance, Acessibilidade, Melhores práticas e SEO) e `curl -I` nos 4 caminhos de F4, registrados em `specs/005-endurecimento-frontend-docs/evidence.md`

## Phase 2: User Story 1 — política de conteúdo e headers (P1) 🎯 MVP

**Goal**: toda resposta recusa enquadramento e traz a CSP gerada da configuração validada.
**Independent Test**: F1, F4 e F8.

- [ ] T002 [US1] Testes primeiro em `scripts/test_runtime_config.py`: CSP com o `authDomain` em `frame-src`; origem absoluta da API em `connect-src` e nenhuma extra com API relativa; emulador só em perfil local; os 7 headers presentes; arquivo de saída injetável por variável (como `ENV_JS_PATH`). Rodar e ver falhar
- [ ] T003 [US1] Em `frontend/docker-entrypoint.sh`, gerar `/etc/nginx/gems/security-headers.conf` a partir dos valores já validados (R1 e R2), com escrita atômica como a do `env.js`; T002 verde
- [ ] T004 [US1] Em `frontend/nginx.conf`, incluir `security-headers.conf` no `server` e em toda `location` com `add_header` própria (R2); ajustar `frontend/Dockerfile` só se o diretório `/etc/nginx/gems` precisar existir na imagem

## Phase 3: User Story 2 — upstream configurável (P1)

**Goal**: o container sobe sem resolver o backend e o proxy segue `API_UPSTREAM`.
**Independent Test**: F3 e F7.

- [ ] T005 [US2] Testes primeiro em `scripts/test_runtime_config.py`: upstream padrão `http://backend:8080`; upstream válido; recusa de esquema, caminho, query, credencial e hostname inválidos, sem eco do valor; `resolv.conf` sem `nameserver` recusado (caminho injetável)
- [ ] T006 [US2] Em `frontend/docker-entrypoint.sh`, gerar `/etc/nginx/gems/api-upstream.conf` (`resolver`, `set $api_upstream`, `proxy_pass $api_upstream`), conforme R5; em `frontend/nginx.conf`, a `location /api/` passa a incluir o arquivo; T005 verde

## Phase 4: User Story 3 — cache e robots (P2)

**Goal**: cache correto e `robots.txt` real.
**Independent Test**: F5 e F6.

- [ ] T007 [P] [US3] Criar `frontend/public/robots.txt` (R4)
- [ ] T008 [US3] Em `frontend/nginx.conf`, `location` para assets com hash (`immutable`), `no-cache` no `index.html` e `location = /robots.txt` (R3 e R4), sempre com o include de headers
- [ ] T009 [US1] [US2] [US3] Smoke do container: `npm run build`, `docker build`, `nginx -t` (F2), subida sem host `backend` (F3), `curl -I` de F4 a F6 e upstream real e inválido (F7); registrar em `evidence.md`
- [ ] T010 [US1] Login em tela com a CSP ativa: senha pelo coordenador e Google pelo product owner (F8); Lighthouse depois (F9); registrar em `evidence.md`
- [ ] T011 [US1] Commit `feat(frontend): headers de segurança, cache, robots e upstream configurável no nginx`

## Phase 5: User Story 4 — documentação e scripts (P2)

**Goal**: documentação fiel ao estado entregue e `.pyc` fora do git.
**Independent Test**: F11 e a leitura do README e do guia.

- [ ] T012 [P] [US4] `README.md`: as linhas 10–11, 89, 98 e 101 passam a refletir o que a 003 e a 006 provaram (R7), com link para as evidências
- [ ] T013 [P] [US4] Guia `specs/003-firebase-multi-tenant/configuracao-homologacao-firebase.md` §5: `APP_SECURITY_CORS_ALLOWED_ORIGINS` com exemplo; seção do frontend em container com `API_UPSTREAM` (R7 e C5)
- [ ] T014 [US4] `git rm --cached scripts/__pycache__/*.pyc`; `__pycache__/` no `.gitignore`; atualizar o hash do `.gitignore` em `scripts/demo-manifest.json` e o pin `MANIFEST_SHA256` de `scripts/sample.py` pelo procedimento da 006 R11; provar F11
- [ ] T015 [US4] Commit `docs: alinha README e guia ao estado entregue e tira os .pyc do git`

## Phase 6: Review, gates e PR

- [ ] T016 Review Sol read-only do diff inteiro: CSP mínima e coerente com R1, ausência de valor não validado no nginx, herança do `add_header`, Compose sem mudança, documentação sem segredo
- [ ] T017 Gates: F10, F11, F12 (A11 da 006 sobre a 005) e F13 local; PR no sample (só com o ok do usuário); CI verde nos 4 jobs; merge pelo usuário
- [ ] T018 Registrar `evidence.md` e fechar a §12 do roteiro; atualizar o `.specify/feature.json`

## Dependencies

```
merge da 006 → T001 → T002 → T003 → T004 → T005 → T006 → {T007} → T008 → T009 → T010 → T011
T001 → {T012, T013} → T014 → T015
T011, T015 → T016 → T017 → T018
```

## Parallel

- T007, T012 e T013 tocam arquivos disjuntos.
- T002–T006 e T008 tocam o mesmo `nginx.conf` ou o mesmo entrypoint: sequenciais.

## Implementation Strategy

MVP = US1 (headers e CSP), o único item com peso de segurança. US2 fecha o achado 2. US3 e US4 completam
os achados 6, 1 e 3.
