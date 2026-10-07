# Tasks: Parceiro e Solicitação

**Formato**: `[ID] [P?] [Story]` — `[P]` marca tarefas paralelizáveis (arquivos diferentes, sem
dependência).

**Convenção de caminho**: backend em `backend/app-<modulo>/src/...`; frontend em
`frontend/src/app/...`.

**Delegação desta rodada: SEM DELEGAÇÃO.** A justificativa está na §8 do
[roteiro.md](roteiro.md#8-divisão-de-tasks-e-delegação-por-modelo) — as fases que decidem o desenho
não amortizam uma sessão nova, e ainda não havia domínio de referência a espelhar. A partir da 002 o
critério muda.

---

## Fase 1 — Reator, changelogs e entidades base

- [x] **T001** POM agregador e os cinco filhos, com o grafo de dependência do Princípio I
- [x] **T002** `EntidadeAuditavel` e `CampoSigiloso` em `app-model/entity/base`
- [x] **T003** [P] `changelog-localidades` + `changelog-normalizacao` + entidades correspondentes
- [x] **T004** [P] `changelog-security` + `Usuario`
- [x] **T005** `changelog-master` com a ordem obrigatória dos includes
- [x] **T006** `AppApplication`, `application.yml` e `AbstractIntegrationTest`

**Checkpoint**: `AppApplicationTests.contextLoads` verde — paridade JPA ↔ Liquibase.

---

## Fase 2 — Transversais: auditoria e autorização

> Teste **antes** da regra em todas as tarefas desta fase (Princípio VII).

- [x] **T007** `AcaoSistema` com as 21 ações
- [x] **T008** `SecurityConfig`: conversão `ROLE_`, varredura de `@PublicEndpoint`, deny do S3
- [x] **T009** `UsuarioAutenticadoService` + `UsuarioAutenticadoServiceTest`
- [x] **T010** `MapaPerfilAcoes` + `MapaPerfilAcoesTest` (contagens transcritas do contrato)
- [x] **T011** `AutorizacaoBootstrap` + `AutorizacaoBootstrapTest` (ordem, totalidade, resiliência)
- [x] **T012** `changelog-auditoria` + entidades da trilha
- [x] **T013** `AuditoriaInterceptor` + `AuditoriaInterceptorTest` (fronteira por tipo)
- [x] **T014** `AuditoriaIsolamentoTest` — nenhum service de domínio referencia o pacote

**Checkpoint**: as varreduras provadas por **mutação** — quebre o alvo, veja o teste falhar.

---

## Fase 3 — Domínio `parceiro`

- [x] **T015** [US1] `changelog-parceiro` + entidade `Parceiro`
- [x] **T016** [US1] `ParceiroRepository` com `POST /search` e projeção por construtor
- [x] **T017** [US4] `ParceiroServiceTest` — os quatro casos do sigilo, **antes** do service
- [x] **T018** [US1] `ParceiroService` na ordem canônica
- [x] **T019** [US1] `ParceiroController` com uma ação por endpoint
- [x] **T020** [US1] `CoberturaPreAuthorizeTest` com a lista fechada de controllers

**Checkpoint**: `mvnw clean install`.

---

## Fase 4 — Domínio `solicitacao` e o evento

- [x] **T021** [US2] `SituacaoSolicitacao` com a tabela de transições
- [x] **T022** [US2] `changelog-solicitacao` — `SOLICITACAO` **antes** de `ETAPA_SOLICITACAO`
- [x] **T023** [US2] `SolicitacaoServiceTest` — transições, conclusão, cancelamento, PUT que ignora
- [x] **T024** [US2] `SolicitacaoService` com as transições e o único ponto que altera a situação
- [x] **T025** [US2] `SolicitacaoController` com os quatro endpoints de transição
- [x] **T026** [US3] `SolicitacaoConcluidaEvent` em `app-events` (POJO)
- [x] **T027** [US3] `SolicitacaoConcluidaListener` em `parceiro.listener`, `BEFORE_COMMIT`

**Checkpoint**: `mvnw clean install` + `AutorizacaoPorAcaoIntegrationTest`.

---

## Fase 5 — Frontend

- [x] **T028** [P] `core/` — auth, guards (com `routes-coverage.spec`), menu (com teste), footer
- [x] **T029** [P] `shared/forms` e `shared/auditoria` — um dono só para o compartilhado
- [x] **T030** [US1][US4] feature `parceiro` — listagem, wizard e o teste do payload da comissão
- [x] **T031** [US2] feature `solicitacao` — listagem e os botões de fluxo

**Checkpoint**: `npm test` + `npm run build`.

---

## Fase 6 — Ambiente e fechamento

- [x] **T032** `environments/local`, realm declarativo, tema, workflows

**Checkpoint**: gate completo + homologação.

---

## Dependências e ordem

```
Fase 1 ──▶ Fase 2 ──▶ Fase 3 ──▶ Fase 4 ──▶ Fase 6
                            └──▶ Fase 5 ──┘
```

Estritamente serial até a 3. `[P]` dentro de uma fase significa arquivos diferentes.

## Estratégia

**MVP = fases 1, 2 e 3** — um cadastro auditado e autorizado já entrega valor. A fase 2 é a que
**não** pode ser cortada: construída depois, seria retrofit sobre os domínios.