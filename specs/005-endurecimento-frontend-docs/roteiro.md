# Roteiro da Rodada — 005 Endurecimento do frontend, documentação e scripts

**Feature**: `005-endurecimento-frontend-docs` · **Branch**: `feature/005-endurecimento-frontend-docs` ·
**Requisito**: achados 1, 2, 3 e 6 da homologação da 003
([evidence.md](../003-firebase-multi-tenant/evidence.md#homologacao-local-20261012)) e testes Python
quebrados em `scripts/`
**Data**: 2026-10-12 · **Status**: fase 2 concluída, aguardando o portão (§12)

**Índice**: [1 Objetivo](#1-objetivo-da-rodada) · [2 Decisões de entrada](#2-decisões-de-entrada) ·
[5 Frontend](#5-frontend-e-container) · [6 Testes](#6-testes-esperados-por-bloco) ·
[7 Isolamento](#7-interação-e-isolamento) · [8 Tasks](#8-divisão-de-tasks-e-delegação-por-modelo) ·
[9 SDD](#9-prompts-sdd-da-rodada) · [10 Riscos](#10-riscos-e-mitigações) · [11 Fora de escopo](#11-fora-de-escopo) ·
[12 Aprovação](#12-critérios-de-aprovação)

Sem §3 (banco) e §4 (backend): a rodada só documenta a variável de CORS do backend, sem mudar código.

---

## 1. Objetivo da rodada

Deixar o container do frontend apto a servir de molde de SaaS com login e alinhar a documentação e
os scripts ao estado real do código. A rodada não muda comportamento de negócio.

### O que entra, por achado

| Origem | Estado verificado em 2026-10-12 (o código vence) | O que a rodada entrega |
| :--- | :--- | :--- |
| Achado 6 | `frontend/nginx.conf` não envia nenhum header de segurança. Só `/env.js` define `Cache-Control`. `frontend/public/robots.txt` não existe, e o fallback do SPA devolve `index.html` no lugar dele | Proteção contra clickjacking (`frame-ancestors`/`X-Frame-Options`), demais headers de segurança compatíveis com o login Google do Firebase, cache longo para assets com hash, `index.html` sem cache e um `robots.txt` |
| Achado 2 | `proxy_pass http://backend:8080` resolve o host `backend` no boot. Fora do Compose, o container só sobe com `--add-host backend:<ip>` | Upstream configurável, sem quebrar o Compose atual |
| Achado 1 | O guia [configuracao-homologacao-firebase.md](../003-firebase-multi-tenant/configuracao-homologacao-firebase.md) não cita `APP_SECURITY_CORS_ALLOWED_ORIGINS` (`app.security.cors.allowed-origins`, `SecurityConfig.java:50`) | A variável documentada no guia |
| Achado 3 | O `README.md` da raiz descreve o E2E, o cleanup e o benchmark como pendentes | README alinhado ao estado da `main` |
| Scripts | `scripts/__pycache__/*.pyc` estão versionados. Os 57 testes Python que falham **saíram desta rodada** (decisão 4) | `__pycache__` fora do git e ignorado |

---

## 2. Decisões de entrada

Decisões tomadas com o product owner em 2026-10-12, antes da escrita da especificação.

| # | Ponto | Decisão | O que isso fecha |
| :-- | :--- | :--- | :--- |
| 1 | Divisão em rodadas | **Duas rodadas.** A [004](../004-reativacao-acao-padrao/roteiro.md) cuida da reativação. A 005 cuida do frontend, da documentação e dos scripts | O PR da 005 não mistura SDK com frontend e não espera release |
| 2 | Testes Python e `.pyc` | **Entram na 005** | Fecha a pendência de manutenção na mesma rodada de alinhamento |
| 3 | Meta de desempenho | **Sem meta de Performance no Lighthouse.** Fala do usuário na 003: "para um projeto de sample, o critério de performance não precisa estar alto". Acessibilidade continua em 100 | O ganho esperado fica em Melhores práticas e SEO, sem perseguir número de performance |
| 4 | Testes Python e manifesto do clean-demo | **Vão para a [006](../006-formatter-gems-ai/roteiro.md)** (2026-10-12, resposta à pergunta de escopo: "Tudo na 006"). Diagnóstico: 48 dos 57 casos são `Protected metadata integrity conflict: backend/app-security/pom.xml`, e o `clean-demo --dry-run` real recusa pelo mesmo motivo. A 006 troca o formatter do `sample.py` e reformata o Java, o que invalida o manifesto de novo | A 005 fica com nginx, `robots.txt`, README, CORS e `.pyc`. Substitui a decisão 2 na parte dos testes |

### Decisões tomadas por convenção, sem pergunta

| # | Ponto | Decisão | Fonte da convenção |
| :-- | :--- | :--- | :--- |
| C1 | Política de headers | A CSP precisa permitir os domínios do Firebase Auth e do login Google. O COOP fica em `same-origin-allow-popups` para não quebrar o popup do Google. HSTS fica fora, porque o sample local roda em HTTP | Achado 6 e o login Google homologado na 003 |
| C2 | Herança do `add_header` no nginx | Os headers são declarados de forma a valer em todas as `location`, inclusive `/env.js` (no nginx, um `add_header` dentro da `location` descarta os do `server`) | Comportamento documentado do nginx |
| C3 | Prova | Smoke do container com `curl -I` nos caminhos `/`, `/assets/*`, `/env.js` e `/robots.txt`. Login Google e por senha repetidos em tela com a CSP ativa. Lighthouse antes e depois na `/login` | Princípio de prova observada. Planejado = NOT_RUN |
| C5 | Guia da 003 | O guia é o documento operacional vigente, então a variável de CORS é acrescentada nele. O restante da spec 003 não muda | `specs/AGENTS.md`: specs concluídas são história, mas o guia continua em uso |
| C6 | Ordem com a 006 | A fase 3 começa depois do merge da 006 no sample, porque a 005 muda o `.gitignore` (metadado do manifesto do clean-demo) e o entrypoint (metadado do inicializador) | [research](research.md) R8 e R9 |

---

## 5. Frontend e container

Nenhum componente Angular muda. O que muda é a borda do container, `frontend/nginx.conf` e
`frontend/docker-entrypoint.sh`, e o `public/`. Os fundamentos estão na [research](research.md).

| Bloco | Mudança | Fonte |
| :--- | :--- | :--- |
| Política de conteúdo | Gerada pelo entrypoint a partir da configuração pública já validada: `frame-src` com o `authDomain`, `connect-src` com a API absoluta (se houver) e o emulador só em perfil local, `script-src` com `apis.google.com` por causa do popup do Google, `style-src 'unsafe-inline'` por causa dos estilos de componente do Angular, `frame-ancestors 'none'` | R1 |
| Demais headers | `X-Frame-Options`, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy`, `Cross-Origin-Opener-Policy: same-origin-allow-popups`, `X-Permitted-Cross-Domain-Policies`; todos `always` e incluídos em toda `location` com `add_header` própria | R2, C2 |
| Cache | Assets com hash: 1 ano, `immutable`; `index.html`: `no-cache`; `/env.js`: `no-store`, como hoje | R3 |
| `robots.txt` | Arquivo novo em `public/`: `Allow: /login`, `Disallow: /`; servido como texto | R4 |
| Upstream | `API_UPSTREAM` (padrão `http://backend:8080`), validado na subida; o proxy resolve o nome na requisição, então o container sobe sem o backend | R5 |
| Documentação | README alinhado ao estado entregue; guia com `APP_SECURITY_CORS_ALLOWED_ORIGINS` e `API_UPSTREAM` | R7, C5 |
| Scripts | `.pyc` fora do git, `__pycache__/` ignorado, hash do `.gitignore` e pin do manifesto atualizados | R8 |

---

## 6. Testes esperados por bloco

| # | Prova | O que ela prova | Novo/existente |
| :-- | :--- | :--- | :--- |
| 1 | `test_runtime_config.py`: CSP com `authDomain`, API absoluta e relativa, emulador só local, 7 headers | A política sai só de valor validado e é a mínima de R1 | novo (T002) |
| 2 | `test_runtime_config.py`: upstream padrão, válido e inválido (sem eco), `resolv.conf` sem `nameserver` | O upstream é fail-closed e não vaza o valor | novo (T005) |
| 3 | `nginx -t` no container no ar | A configuração gerada é válida | novo (F2) |
| 4 | Container sem host `backend`: `/healthz` 200 | O achado 2 foi corrigido | novo (F3) |
| 5 | `curl -I` em 4 caminhos | Os headers valem em toda `location` (herança do `add_header`) e o cache é o de R3 | novo (F4, F5) |
| 6 | `GET /robots.txt` | Não cai no fallback do SPA | novo (F6) |
| 7 | Login Google e por senha em tela, sem violação no console | A CSP não quebra o login | novo (F8; Google pelo product owner) |
| 8 | Lighthouse na `/login` antes e depois | Melhores práticas e SEO sobem; Acessibilidade continua em 100 | novo (F9) |
| 9 | Testes Python (60), `clean-demo --dry-run` e A11 da 006 | O `.gitignore` e o entrypoint novos não quebram o clean-demo nem o inicializador | existente (F11, F12) |
| 10 | `npm test`, `npm run build`, `prettier --check`, gate GEMS e CI | Zero regressão | existente (F10, F13) |

Do §C do checklist de fechamento, aplica-se o gate do frontend e dos scripts. Isolamento por tenant,
autorização e estado assíncrono não mudam, porque nenhum código de backend ou de componente é tocado.

---

## 7. Interação e isolamento

| Recurso | Classificação | O que impede regressão |
| :--- | :--- | :--- |
| `frontend/nginx.conf` | alterado compartilhado | Provas 3 a 7 |
| `frontend/docker-entrypoint.sh` | alterado compartilhado | Provas 1, 2 e 9 (é metadado do inicializador) |
| `frontend/public/robots.txt` | novo isolado | Prova 6 |
| `.gitignore` e `scripts/demo-manifest.json` | alterado compartilhado | Prova 9 |
| README e guia da 003 | alterado compartilhado | Leitura no review |
| Compose local | reuso sem alteração | O padrão de `API_UPSTREAM` é o endereço de hoje |
| Backend e componentes Angular | reuso sem alteração | Prova 10 |

### O que fica frouxo depois desta feature

- **`style-src 'unsafe-inline'`.** Os estilos de componente do Angular exigem isso sem nonce. O risco é
  baixo, porque `script-src` não tem `unsafe-inline`.
- **Sem HSTS.** O sample local roda em HTTP (C1). Quem publicar com HTTPS precisa acrescentar o header
  no proxy de borda.
- **Domínios do Google fixos na política.** Se o SDK Web do Firebase passar a usar outro domínio, o login
  quebra com violação no console. A prova 7 pega isso na hora da atualização.
- **A resolução por requisição depende do `resolv.conf` do container.** Num ambiente sem `nameserver`, a
  subida recusa em vez de ficar sem upstream.

---

## 8. Divisão de tasks e delegação por modelo

**18 tarefas** em 6 fases ([tasks.md](tasks.md)). Distribuição: US1 6 (T002–T004, T009–T011), US2 2
(T005–T006), US3 2 (T007–T008), US4 4 (T012–T015) e infraestrutura, review e fechamento 4.

| Fase | Escopo | Tarefas | Executor | Gate de saída |
| :-- | :--- | :--- | :--- | :--- |
| 1 | Rebase e linha de base | T001 | Coordenador | Lighthouse e `curl -I` de antes registrados |
| 2–4 | Headers, CSP, upstream, cache e robots | T002–T011 | Sol medium (T002–T008) · coordenador (smoke) · **product owner** (login Google) | Provas 1 a 8 |
| 5 | Documentação e scripts | T012–T015 | Coordenador | Prova 9 |
| 6 | Review, gates e PR | T016–T018 | Sol read-only · coordenador · **usuário** (ok de PR e merge) | CI verde |

### Critério de delegação

Custo mínimo sujeito aos gates. A borda do nginx e o entrypoint são superfície de segurança, com teste
primeiro e smoke como gate. Por isso vão para o Sol medium e passam por review independente. Texto e
manifesto ficam com o coordenador, porque são curtos e já têm procedimento provado na 006. Nenhum uso
de Opus como executor.

### Ordem e paralelismo

```
merge da 006 → T001 → (T002→T011) ─┐
              T001 → (T012→T015) ──┴→ T016 → T017 → T018
```

---

## 9. Prompts SDD da rodada

| Ordem | Comando | Situação | Saída |
| :-- | :--- | :--- | :--- |
| 1 | `/speckit-specify` | ✅ 2026-10-12 | [spec.md](spec.md), [checklist](checklists/requirements.md) 100% |
| 2 | `/speckit-clarify` | ✅ dispensado | As decisões 1–4 foram fechadas com o product owner; as demais são convenções (C1–C6). A spec saiu sem `[NEEDS CLARIFICATION]` |
| 3 | `/speckit-plan` | ✅ 2026-10-12 | [plan.md](plan.md), [research.md](research.md) (R1–R9), [quickstart.md](quickstart.md). Sem `data-model.md` e sem `contracts/` |
| 4 | `/speckit-tasks` | ✅ 2026-10-12 | [tasks.md](tasks.md), 18 tarefas |
| 5 | `/speckit-analyze` | ✅ 2026-10-12 | 0 CRITICAL, 0 HIGH, 2 MEDIUM e 1 LOW, todos incorporados |
| 6 | **`roteiro.md`** | ✅ | este documento |
| 7 | `/speckit-implement` | 🔒 | após aprovação e depois do merge da 006 |

### Achados do `/speckit-analyze` já incorporados

| Achado | Sev. | Decisão |
| :--- | :--- | :--- |
| U1: `nginx -t` num `docker run --rm` pula o entrypoint, e os includes gerados não existiriam | MEDIUM | F2 roda com `docker exec` no container no ar |
| C1: o `.gitignore` é metadado protegido do manifesto do clean-demo, e o entrypoint é metadado do inicializador | MEDIUM | T014 atualiza o hash e o pin; F11 e F12 (A11 da 006) entram no gate; C6 fixa a ordem |
| A1: o login Google em tela depende do product owner | LOW | T010 e prova 7 dizem quem executa |

---

## 10. Riscos e mitigações

| # | Risco | Gravidade | Mitigação |
| :-- | :--- | :--- | :--- |
| 1 | A CSP quebra o login Google ou por senha | alta | Prova 7 em tela, antes do PR; domínio novo só com a evidência da violação |
| 2 | Valor de ambiente injeta diretiva no nginx | alta | A política e o upstream saem só de valores já validados pelo entrypoint (provas 1 e 2) |
| 3 | `add_header` numa `location` apaga os headers herdados | média | Include em toda `location` com `add_header` própria; prova 5 nos 4 caminhos |
| 4 | O upstream por variável muda o caminho repassado ao backend | média | `proxy_pass` sem URI; F7 com chamada real em `/api/` |
| 5 | Mudar o `.gitignore` quebra o clean-demo de novo | média | T014 e prova 9 |

---

## 11. Fora de escopo

| Item | Por quê | Volta em |
| :--- | :--- | :--- |
| HSTS | O sample local roda em HTTP (C1) | na publicação com HTTPS |
| Meta de Performance no Lighthouse | Decisão 3 | não volta |
| Nonce de CSP ou remover `'unsafe-inline'` de `style-src` | Exige mudança no build do Angular | rodada própria, se o product owner pedir |
| 404 para todo arquivo estático ausente (além do `robots.txt`) | Muda o fallback do SPA | rodada própria |
| Mudanças no Compose | O padrão do upstream mantém o comportamento | não volta |

---

## 12. Critérios de aprovação

### Para aprovar esta rodada (antes do código)

- [x] O escopo da §1 é o que se espera da próxima entrega
- [x] As decisões da §2 refletem o que foi combinado
- [x] As mudanças da §5, em especial a política de conteúdo, são aceitáveis
- [x] O impacto da §7 é aceitável, inclusive o que fica frouxo
- [x] A cobertura de provas da §6 é suficiente para o risco assumido
- [x] A decisão de delegação da §8 está adequada
- [x] Os itens fora de escopo (§11) podem mesmo esperar

> ✅ **PORTÃO APROVADO pelo product owner em 2026-10-12.** Resposta literal à pergunta do portão, que
> resumia a CSP gerada na subida do container, os 7 headers, o cache imutável dos assets, o upstream
> configurável e o `robots.txt`: "Aprovo a fase 2". A implementação começa sobre a `main` com a 006
> (`f768fbe`, merge do PR #12), conforme a condição C6. Push, PR e merge continuam exigindo ok próprio.
> **O `/speckit-implement` está liberado.**

### Para considerar a rodada concluída (depois do código)

- [ ] Testes de runtime verdes, com os casos novos de CSP e upstream
- [ ] Container saudável sem host `backend`; `nginx -t` ok; 7 headers nos 4 caminhos; cache e robots como R3 e R4
- [ ] Login Google e por senha em tela com a CSP, sem violação
- [ ] Lighthouse: Acessibilidade 100, Melhores práticas e SEO acima de 77 e 83
- [ ] Testes Python (60), `clean-demo --dry-run` e A11 da 006 verdes
- [ ] `npm test`, `npm run build`, `prettier --check`, gate GEMS e CI verdes
- [ ] **Zero regressão** nas features já entregues

---

## Referência dos artefatos

| Arquivo | Para quê |
| :--- | :--- |
| [spec.md](spec.md) | Comportamento, requisitos e critérios de sucesso |
| [research.md](research.md) | Fatos apurados e decisões técnicas (R1–R9) |
| [plan.md](plan.md) | Contexto técnico, constituição e delegação |
| [quickstart.md](quickstart.md) | Gates F1–F13 |
| [tasks.md](tasks.md) | As 18 tarefas |
