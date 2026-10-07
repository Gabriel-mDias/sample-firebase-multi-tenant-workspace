# Implementation Plan: Parceiro e Solicitação

**Branch**: `feature/001-parceiro-e-solicitacao` · **Spec**: [spec.md](spec.md) · **Date**: 2026-09-05

## Summary

Entregar dois domínios sobre a infraestrutura transversal que eles exigem. `parceiro` é o cadastro de
referência: agregados normalizados sem cascade, validação acumulada, e a autorização de campo da
comissão. `solicitacao` é o domínio de fluxo: máquina de estados no enum, situação que só avança por
endpoint de ação, e um evento de domínio que fecha o ciclo com `parceiro` sem criar dependência de
código na volta.

A auditoria e a autorização por ações entram **nesta** rodada, antes dos domínios — construídas
depois, teriam de ser retrofitadas sobre eles.

## Technical Context

**Language/Version**: Java 21 · TypeScript 6
**Primary Dependencies**: Spring Boot 4.1, GEMS SDK 2.0.0, Angular 22, `angular-gems-sdk` 1.1.0
**Storage**: PostgreSQL 16, schemas `public` / `security` / `auditoria`, migração por Liquibase
**Identity**: Keycloak 26 como resource server JWT; perfis são grupos
**Testing**: JUnit 5 + Mockito + Testcontainers (backend) · Vitest (frontend)
**Target Platform**: Docker Compose local; VPS com Caddy em produção
**Project Type**: web — reator Maven de 5 módulos + SPA Angular
**Performance Goals**: nenhuma meta numérica nesta rodada; a listagem é paginada por desenho
**Constraints**: `ddl-auto: validate`; `open-in-view: false`; eventos em memória
**Scale/Scope**: 3 tabelas de negócio novas, 14 endpoints, 2 features de frontend

## Constitution Check

| Princípio | Como esta rodada o cumpre |
| :--- | :--- |
| I — Fronteiras pelo reator | `app-events` só POJOs; o listener da conclusão vive em `app-core`. `solicitacao` lê `ParceiroRepository` (consulta) e não escreve nele (evento) |
| II — Persistência declarada | `ddl-auto: validate`; UUID; soft delete via `@SQLDelete` + `@SQLRestriction`; `ManyToOne` LAZY; prefixos de coluna respeitados |
| III — Changelog rastreável | Master é só índice; um arquivo por domínio; `id` descritivo; seed idempotente de país e UF |
| IV — Validação acumulada e ordem canônica | Todos os services seguem `findById > search > delete > insert > save > validate`; `validate` acumula e lança uma vez |
| V — Contrato REST e autorização | `POST /search`; uma ação concreta por endpoint; autorização de campo no service; UI espelha a ação; as duas varreduras existem |
| VI — Frontend modular | Feature autocontida por `loadChildren`; standalone; signals; store `GemsBaseStore`; `app-field-error`; tema só no provider |
| VII — Testes como gate | Regra de negócio com teste antes; redes novas provadas por mutação; gate de merge verde |
| VIII — Spec antes do código | Ciclo completo percorrido; este roteiro aprovado antes do `implement`; delegação decidida explicitamente |
| IX — Ambiente é código | Compose e realm versionados; `IMPORT_CACHE_ENABLED=false`; `.env.example` como contrato |

**Nenhum desvio.** Complexity Tracking vazio.

## Project Structure

### Documentation

```
specs/001-parceiro-e-solicitacao/
├── spec.md
├── plan.md              # este arquivo
├── research.md
├── data-model.md
├── quickstart.md
├── tasks.md
├── roteiro.md           # o portão humano
├── contracts/
│   ├── rest-api.md
│   └── frontend-module.md
└── checklists/
    └── requirements.md
```

### Source Code

```
backend/
├── app-model/      entity/{base,auditoria,localidades,normalizacao,seguranca,parceiro,solicitacao}
│                   dto/ · enums/ · params/ · response/
├── app-security/   AcaoSistema · SecurityConfig · services do Keycloak · UsuarioAutenticadoService
├── app-events/     solicitacao/event · usuario/event · normalizacao/event   (somente POJOs)
├── app-core/       auditoria/ · autorizacao/ · normalizacao/ · localidade/ · usuario/ · commons/
│                   parceiro/{controller,service,repository,listener}
│                   solicitacao/{controller,service,repository}
└── app-deploy/     AppApplication · application.yml · db/changelog/ · testes de integração

frontend/src/app/
├── core/           auth · guards · menu · footer · localidade
├── commons/        constantes e funções puras
├── shared/         forms/ · auditoria/ · components/
└── features/       parceiro/ · solicitacao/ · home/ · acesso-negado/
```

## Complexity Tracking

Vazio. Nenhuma decisão desta rodada diverge de um princípio.
