# Implementation Plan: Endurecimento do frontend, documentação e scripts

**Branch**: `feature/005-endurecimento-frontend-docs` (rebaseada sobre a `main` depois do merge da 006) |
**Date**: 2026-10-12 | **Spec**: [spec.md](spec.md)

## Summary

O entrypoint do container do frontend, que já valida a configuração pública, passa a gerar a política de
conteúdo e o upstream da API para o nginx. O `nginx.conf` inclui os headers em todas as `location`,
define cache longo para assets com hash e revalidação para o `index.html`, e serve um `robots.txt` real.
O README e o guia da 003 passam a refletir o estado real, e os `.pyc` saem do git, com o hash do
`.gitignore` atualizado no manifesto do clean-demo. Fundamentos em [research.md](research.md).

## Technical Context

**Language/Version**: nginx 1.28 (imagem `nginx:1.28-alpine`); Python 3 do entrypoint (já existente); Angular 22 (só o `public/`)
**Primary Dependencies**: nenhuma nova
**Storage**: N/A
**Testing**: `scripts/test_runtime_config.py` (unittest, estendido); smoke do container com `curl -I` e `nginx -t`; Lighthouse na `/login`; login em tela; gates do frontend; unittest de `scripts/` (60) e A11 da 006
**Target Platform**: container Docker do frontend; Compose local
**Project Type**: web (só a borda do frontend e documentação)
**Performance Goals**: sem meta de Performance (decisão 3)
**Constraints**: nenhum valor não validado na configuração do nginx; Compose sem mudança; HSTS fora (C1)
**Scale/Scope**: 3 arquivos de runtime (`nginx.conf`, `docker-entrypoint.sh`, `Dockerfile` se preciso), 1 arquivo novo em `public/`, 1 teste estendido, 2 documentos, `.gitignore` e manifesto

## Constitution Check

| Princípio | Situação |
| :--- | :--- |
| I–IV. Backend, persistência, serviço | ✅ Não tocados. Só a documentação da variável de CORS |
| V. REST | ✅ O proxy continua repassando `/api/` com o mesmo caminho |
| VI. Frontend | ✅ Nenhum componente muda; só `public/robots.txt` |
| VII. Testes como gate | ✅ Teste de runtime estendido, smoke do container, login em tela e zero regressão nos gates |
| VIII. Especificação antes de código | ✅ Este ciclo e o portão da §12 |
| IX. Configuração é código | ✅ Headers, cache e upstream versionados; política gerada de configuração validada |
| Governance: waiver | ✅ Nenhum |

Reavaliação pós-design: sem violação.

## Project Structure

### Documentation (this feature)

```text
specs/005-endurecimento-frontend-docs/
├── roteiro.md, spec.md, plan.md, research.md, quickstart.md, tasks.md
└── checklists/requirements.md
```

Sem `data-model.md` (não há dado) e sem `contracts/`. A interface nova é a variável `API_UPSTREAM`,
descrita na research R5 e no guia.

### Source Code (arquivos tocados)

```text
frontend/
├── nginx.conf                 (includes, cache, robots, proxy pelo upstream gerado)
├── docker-entrypoint.sh       (gera security-headers.conf e api-upstream.conf)
├── Dockerfile                 (diretório /etc/nginx/gems, se o entrypoint não puder criá-lo)
└── public/robots.txt          (novo)
scripts/
├── test_runtime_config.py     (casos novos)
├── __pycache__/*.pyc          (saem do git)
├── demo-manifest.json         (hash do .gitignore)
└── sample.py                  (pin MANIFEST_SHA256)
.gitignore                     (__pycache__/)
README.md
specs/003-firebase-multi-tenant/configuracao-homologacao-firebase.md   (variável de CORS e API_UPSTREAM)
```

**Structure Decision**: dois commits. Um `feat(frontend)` com headers, cache, robots, upstream e testes.
Um `docs` com README, guia e `.pyc`, junto com o `.gitignore` e o manifesto.

## Delegação

| Trabalho | Executor | Razão |
| :--- | :--- | :--- |
| `nginx.conf`, entrypoint e testes de runtime | Sol (Codex) medium | Mexe na fronteira de segurança do frontend, com testes e smoke como gate |
| README, guia, `.gitignore` e `.pyc` | Coordenador | São edições de texto curtas, e o onboarding não se amortiza |
| Manifesto (hash do `.gitignore` e pin) | Coordenador | O procedimento é o da 006 (R11), com a mesma prova |
| Review independente | Sol read-only | Há flag de segurança (headers e CSP) |
| Login Google em tela | Product owner | Verificação humana, como na 003 |

Nenhum uso de Opus como executor.

## Complexity Tracking

Sem violação de constituição a justificar.
