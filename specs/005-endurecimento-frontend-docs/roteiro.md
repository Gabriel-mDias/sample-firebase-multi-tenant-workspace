# Roteiro da Rodada — 005 Endurecimento do frontend, documentação e scripts

**Feature**: `005-endurecimento-frontend-docs` · **Branch**: `feature/005-endurecimento-frontend-docs` ·
**Requisito**: achados 1, 2, 3 e 6 da homologação da 003
([evidence.md](../003-firebase-multi-tenant/evidence.md#homologacao-local-20261012)) e testes Python
quebrados em `scripts/`
**Data**: 2026-10-12 · **Status**: fase 1 concluída, aguardando o ciclo spec-kit (fase 2)

**Índice**: [1 Objetivo](#1-objetivo-da-rodada) · [2 Decisões de entrada](#2-decisões-de-entrada)

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
